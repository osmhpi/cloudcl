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
  public int[] resultSumQty = new int[6];
  public int[] resultSumBasePrice = new int[6];
  public int[] resultSumDiscPrice = new int[6];
  public int[] resultSumCharge = new int[6];
  public int[] resultSumDiscount = new int[6];
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

    for (int g = 0; g < 6; g++) {
      localSumQty[6 * localId + g] = 0;
      localSumBasePrice[6 * localId + g] = 0;
      localSumDiscPrice[6 * localId + g] = 0;
      localSumCharge[6 * localId + g] = 0;
      localSumDiscount[6 * localId + g] = 0;
      localCountOrder[6 * localId + g] = 0;
    }


    // VERY CRUDE APPROXIMATION - Should not use floats, etc.!
    if (colShippingDate[globalId] <= 19980902) {
      int groupByIndex = colReturnFlag[globalId] * 2 + colLineStatus[globalId];
      // FIXME: This can overflow, results should rather be long[],
      //        but Aparapi only has atomicAdd(int[], ...)!
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

    // Group leader stores final reduced sum of group to result buffer
    if (localId == 0) {
      for (int g = 0; g < 6; g++) {
        atomicAdd(resultSumQty, g, localSumQty[g]);
        atomicAdd(resultSumBasePrice, g, localSumBasePrice[g]);
        atomicAdd(resultSumDiscPrice, g, localSumDiscPrice[g]);
        atomicAdd(resultSumCharge, g, localSumCharge[g]);
        atomicAdd(resultSumDiscount, g, localSumDiscount[g]);
        atomicAdd(resultCountOrder, g, localCountOrder[g]);
      }
    }
  }
}
