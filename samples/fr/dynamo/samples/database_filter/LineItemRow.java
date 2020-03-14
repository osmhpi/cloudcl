package fr.dynamo.samples.database_filter;

public final class LineItemRow {
    public final int colQuantity;
    public final int colExtendedPrice; // Fixed-point with 2 decimals (real value times 100)
    public final int colDiscount; // Fixed-point with 2 decimals (real value times 100)
    public final int colTax; // Fixed-point with 2 decimals (real value times 100)
    public final int colReturnFlag; // A = 0, N = 1, R = 2
    public final int colLineStatus; // F = 0, O = 1
    public final int colShippingDate; // In ISO8601 format

    public LineItemRow(int colQuantity,
                       int colExtendedPrice,
                       int colDiscount,
                       int colTax,
                       int colReturnFlag,
                       int colLineStatus,
                       int colShippingDate) {
        this.colQuantity = colQuantity;
        this.colExtendedPrice = colExtendedPrice;
        this.colDiscount = colDiscount;
        this.colTax = colTax;
        this.colReturnFlag = colReturnFlag;
        this.colLineStatus = colLineStatus;
        this.colShippingDate = colShippingDate;
    }
}