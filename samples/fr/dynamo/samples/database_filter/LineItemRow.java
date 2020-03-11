package fr.dynamo.samples.database_filter;

public final class LineItemRow {
    public final int colOrderKey;
    public final int colPartKey;
    public final int colSuppKey;
    public final int colLineNumber;
    public final int colQuantity;
    public final int colExtendedPrice; // Fixed-point with 2 decimals (real value times 100)
    public final int colDiscount; // Fixed-point with 2 decimals (real value times 100)
    public final int colTax; // Fixed-point with 2 decimals (real value times 100)
    public final int colReturnFlag; // A = 0, N = 1, R = 2
    public final int colLineStatus; // F = 0, O = 1
    public final int colShippingDate; // In ISO9660 format
    public final int colCommitDate; // In ISO9660 format
    public final int colReceiptDate; // In ISO9660 format
    public final byte[] colShippingInstructions;
    public final byte[] colShippingMode;
    public final byte[] colComment;

    public LineItemRow(int colOrderKey,
                       int colPartKey,
                       int colSuppKey,
                       int colLineNumber,
                       int colQuantity,
                       int colExtendedPrice,
                       int colDiscount,
                       int colTax,
                       int colReturnFlag,
                       int colLineStatus,
                       int colShippingDate,
                       int colCommitDate,
                       int colReceiptDate,
                       byte[] colShippingInstructions,
                       byte[] colShippingMode,
                       byte[] colComment) {
        this.colOrderKey = colOrderKey;
        this.colPartKey = colPartKey;
        this.colSuppKey = colSuppKey;
        this.colLineNumber = colLineNumber;
        this.colQuantity = colQuantity;
        this.colExtendedPrice = colExtendedPrice;
        this.colDiscount = colDiscount;
        this.colTax = colTax;
        this.colReturnFlag = colReturnFlag;
        this.colLineStatus = colLineStatus;
        this.colShippingDate = colShippingDate;
        this.colCommitDate = colCommitDate;
        this.colReceiptDate = colReceiptDate;
        this.colShippingInstructions = colShippingInstructions;
        this.colShippingMode = colShippingMode;
        this.colComment = colComment;
    }
}