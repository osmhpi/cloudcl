package fr.dynamo.samples.database_filter;
import com.amd.aparapi.Range;

import fr.dynamo.threading.DynamoJob;
import fr.dynamo.threading.DynamoKernel;

public class DatabaseFilterKernel extends DynamoKernel{
  final int size;

  public final LineItemRow[] lines;

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
  public int[] resultSumQty;
  public int[] resultSumBasePrice;
  public int[] resultSumDiscPrice;
  public int[] resultSumCharge;
  public int[] resultSumDiscount;
  public int[] resultCountOrder;

  public DatabaseFilterKernel(DynamoJob job, Range range,
    LineItemRow[] lines, int size) {
    
    super(job, range);
    this.lines = lines;
    this.size = size;
    this.resultSumQty = new int[6];
    this.resultSumBasePrice = new int[6];
    this.resultSumDiscPrice = new int[6];
    this.resultSumCharge = new int[6];
    this.resultSumDiscount = new int[6];
    this.resultCountOrder = new int[6];
  }

  @Override
  public void run() {
    int r = getGlobalId();

    // VERY CRUDE APPROXIMATION - Should not use floats, etc.!
    if (lines[r].colShippingDate <= 19980902) {
      int groupByIndex = lines[r].colReturnFlag * 2 + lines[r].colLineStatus;
      // FIXME: This can overflow, results should rather be long[],
      //        but Aparapi only has atomicAdd(int[], ...)!
      atomicAdd(resultSumQty, groupByIndex, lines[r].colQuantity);
      atomicAdd(resultSumBasePrice, groupByIndex, lines[r].colExtendedPrice);
      atomicAdd(resultSumDiscPrice, groupByIndex, (int)(
        (lines[r].colExtendedPrice / 100.0 * (1.0 - lines[r].colDiscount / 100.0)) * 100.0));
      atomicAdd(resultSumCharge, groupByIndex, (int)(
        (lines[r].colExtendedPrice / 100.0 * (1.0 - lines[r].colDiscount / 100.0)
                                           * (1.0 + lines[r].colTax / 100.0)) * 100.0));
      atomicAdd(resultSumDiscount, groupByIndex, lines[r].colDiscount);
      atomicAdd(resultCountOrder, groupByIndex, 1);
    }
  }
}
