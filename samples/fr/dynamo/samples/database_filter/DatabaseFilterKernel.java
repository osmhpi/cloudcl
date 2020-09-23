package fr.dynamo.samples.database_filter;
import com.amd.aparapi.Range;

import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class DatabaseFilterKernel extends DynamoKernel {

  public final int[] colQuantity;
  public final int[] colExtendedPrice; // Fixed-point with 2 decimals (real value times 100)
  public final int[] colDiscount; // Fixed-point with 2 decimals (real value times 100)
  public final int[] colTax; // Fixed-point with 2 decimals (real value times 100)
  public final int[] colReturnFlag; // A = 0, N = 1, R = 2
  public final int[] colLineStatus; // F = 0, O = 1
  public final int[] colShippingDate; // In ISO8601 format

  /*
  TPC-H QUERY 1
  =============
  SELECT
    l_returnflag,
    l_linestatus,
    sum(l_quantity) AS sum_qty,
    sum(l_extendedprice) AS sum_base_price,
    sum(l_extendedprice*(1-l_discount)) AS sum_disc_price,
    sum(l_extendedprice*(1-l_discount)*(1+l_tax)) AS sum_charge,
    avg(l_quantity) AS avg_qty,
    avg(l_extendedprice) AS avg_price,
    avg(l_discount) AS avg_disc,
    count(*) AS count_order
  FROM
    lineitem
  WHERE
    l_shipdate <= DATE '1998-12-01' - INTERVAL '90' DAY (3)
  GROUP BY
    l_returnflag,
    l_linestatus
  ORDER BY
    l_returnflag,
    l_linestatus;
  */
  // Each of those arrays is indexed by (l_returnflag, l_linestatus)
  public int[] resultSumQtyHi = new int[6];
  public int[] resultSumQtyLo = new int[6];
  public int[] resultSumBasePriceHi = new int[6];
  public int[] resultSumBasePriceLo = new int[6];
  public int[] resultSumDiscPriceHi = new int[6];
  public int[] resultSumDiscPriceLo = new int[6];
  public int[] resultSumChargeHi = new int[6];
  public int[] resultSumChargeLo = new int[6];
  public int[] resultSumDiscountHi = new int[6];
  public int[] resultSumDiscountLo = new int[6];
  public int[] resultCountOrder = new int[6];

  @Local private int[] localSumQty = new int[6 * range.getLocalSize(0)];
  @Local private int[] localSumBasePrice = new int[6 * range.getLocalSize(0)];
  @Local private int[] localSumDiscPrice = new int[6 * range.getLocalSize(0)];
  @Local private int[] localSumCharge = new int[6 * range.getLocalSize(0)];
  @Local private int[] localSumDiscount = new int[6 * range.getLocalSize(0)];
  @Local private int[] localCountOrder = new int[6 * range.getLocalSize(0)];

  public DatabaseFilterKernel(DynamoJob job, Range range, int[] colQuantity, int[] colExtendedPrice, int[] colDiscount,
                              int[] colTax, int[] colReturnFlag, int[] colLineStatus, int[] colShippingDate) {
    super(job, range);
    this.colQuantity = colQuantity;
    this.colExtendedPrice = colExtendedPrice;
    this.colDiscount = colDiscount;
    this.colTax = colTax;
    this.colReturnFlag = colReturnFlag;
    this.colLineStatus = colLineStatus;
    this.colShippingDate = colShippingDate;
  }

  @Override
  public void run() {
    int globalId = getGlobalId(), localId = getLocalId(), localSize = getLocalSize();

    // Initialize workgroup local arrays
    for (int g = 0; g < 6; g++) {
      localSumQty[6 * localId + g] = 0;
      localSumBasePrice[6 * localId + g] = 0;
      localSumDiscPrice[6 * localId + g] = 0;
      localSumCharge[6 * localId + g] = 0;
      localSumDiscount[6 * localId + g] = 0;
      localCountOrder[6 * localId + g] = 0;
    }


    // Aggregate results in the workgroup local array (work items work independently)
    if (colShippingDate[globalId] <= 19980902) {
      int groupByIndex = colReturnFlag[globalId] * 2 + colLineStatus[globalId];
      localSumQty[6 * localId + groupByIndex] = colQuantity[globalId];
      localSumBasePrice[6 * localId + groupByIndex] = colExtendedPrice[globalId];
      localSumDiscPrice[6 * localId + groupByIndex] = (int)(
              (colExtendedPrice[globalId] / 100.0 * (1.0 - colDiscount[globalId] / 100.0)) * 100.0);
      localSumCharge[6 * localId + groupByIndex] = (int)(
              (colExtendedPrice[globalId] / 100.0 * (1.0 - colDiscount[globalId] / 100.0)
                      * (1.0 + colTax[globalId] / 100.0)) * 100.0);
      localSumDiscount[6 * localId + groupByIndex] = colDiscount[globalId];
      localCountOrder[6 * localId + groupByIndex] = 1;
    }

    // Reduce the results in the workgroup local arrays in parallel
    for (int i = localSize / 2; i > 0; i >>= 1) {
      localBarrier();

      // Every iteration half of the threads wrt. the previous iteration
      // accumulate two values while the other half do nothing
      if (localId < i) {
        for (int g = 0; g < 6; g++) {
          localSumQty[6 * localId + g] += localSumQty[6 * (localId + i) + g];
          localSumBasePrice[6 * localId + g] += localSumBasePrice[6 * (localId + i) + g];
          localSumDiscPrice[6 * localId + g] += localSumDiscPrice[6 * (localId + i) + g];
          localSumCharge[6 * localId + g] += localSumCharge[6 * (localId + i) + g];
          localSumDiscount[6 * localId + g] += localSumDiscount[6 * (localId + i) + g];
          localCountOrder[6 * localId + g] += localCountOrder[6 * (localId + i) + g];
        }
      }
    }

    // The group leader aggregates the final reduced sums to the result buffer
    if (localId == 0) {
      for (int g = 0; g < 6; g++) {
        int r;

        // NB: Aparapi hasn't atomicAdd(long[], int, long), so we need to
        // simulate this inefficiently with two separate atomicAdds
        r = atomicAdd(resultSumQtyLo, g, localSumQty[g]);
        if (r < 0 && r + localSumQty[g] >= 0)
          atomicAdd(resultSumQtyHi, g, 1);

        r = atomicAdd(resultSumBasePriceLo, g, localSumBasePrice[g]);
        if (r < 0 && r + localSumBasePrice[g] >= 0)
          atomicAdd(resultSumBasePriceHi, g, 1);

        r = atomicAdd(resultSumDiscPriceLo, g, localSumDiscPrice[g]);
        if (r < 0 && r + localSumDiscPrice[g] >= 0)
          atomicAdd(resultSumDiscPriceHi, g, 1);

        r = atomicAdd(resultSumChargeLo, g, localSumCharge[g]);
        if (r < 0 && r + localSumCharge[g] >= 0)
          atomicAdd(resultSumChargeHi, g, 1);

        r = atomicAdd(resultSumDiscountLo, g, localSumDiscount[g]);
        if (r < 0 && r + localSumDiscount[g] >= 0)
          atomicAdd(resultSumDiscountHi, g, 1);

        atomicAdd(resultCountOrder, g, localCountOrder[g]); // This one can't overflow
      }
    }
  }
}
