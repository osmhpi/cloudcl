package fr.dynamo.samples.database_filter;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;
import java.io.*;

import com.amd.aparapi.Range;

import fr.dynamo.DevicePreference;
import fr.dynamo.ThreadFinishedNotifyable;
import fr.dynamo.threading.DynamoJob;

public class DatabaseFilterJob extends DynamoJob{

  public DatabaseFilterJob(String sizeOrTpchLineItemFile, int tiles, DevicePreference preference, ThreadFinishedNotifyable notifyable) throws IOException {
    super("DatabaseFilter", notifyable);

    boolean useFile = false;
    try {
      Integer.parseInt(sizeOrTpchLineItemFile);
    } catch(NumberFormatException e) {
      useFile = true;
    }

    LineItemRow[] lines;
    int size;

    if (useFile) {
      System.out.println("Counting lines...");
      try (FileReader input = new FileReader(sizeOrTpchLineItemFile);
           LineNumberReader count = new LineNumberReader(input)) {
        while (count.skip(Long.MAX_VALUE) > 0) {
        }
        size = count.getLineNumber();
      }

      System.out.println("Reading " + size + " lines...");
      lines = new LineItemRow[size];
      try (FileReader input = new FileReader(sizeOrTpchLineItemFile);
           BufferedReader br = new BufferedReader(input)) {

        int r = 0;
        for (String line = br.readLine(); line != null; line = br.readLine()) {
          String[] lineParts = line.split("\\|");
          int colQuantity = Integer.parseInt(lineParts[4]);
          int colExtendedPrice = (int) (Float.parseFloat(lineParts[5]) * 100);
          int colDiscount = (int) (Float.parseFloat(lineParts[6]) * 100);
          int colTax = (int) (Float.parseFloat(lineParts[7]) * 100);
          int colReturnFlag = lineParts[8].equals("A") ? 0 :
                  lineParts[8].equals("N") ? 1 :
                          lineParts[8].equals("R") ? 2 :
                                  -1;
          int colLineStatus = lineParts[9].equals("F") ? 0 :
                  lineParts[9].equals("O") ? 1 :
                          -1;
          int colShippingDate = Integer.parseInt(lineParts[10].replace("-", ""));

          lines[r++] = new LineItemRow(colQuantity, colExtendedPrice, colDiscount, colTax,
                  colReturnFlag, colLineStatus, colShippingDate);
        }
      }
    } else {
      size = Integer.parseInt(sizeOrTpchLineItemFile);

      Random rng = new Random(12345);
      SimpleDateFormat yyyymmddDateFormat = new SimpleDateFormat("yyyyMMdd");

      System.out.println("Generating " + size + " lines...");
      lines = new LineItemRow[size];

      for (int i = 0; i < size; i++) {
        int colQuantity = 1 + rng.nextInt(50); // [1, 50]
        int colDiscount = rng.nextInt(11); // [0, 10]
        int colTax = rng.nextInt(9); // [0, 8]
        int unitPrice = 90000 + rng.nextInt(100001); // [90000,190000], aprox.
        int colExtendedPrice = unitPrice * colQuantity;
        Calendar c = Calendar.getInstance();
        c.set(1992, Calendar.JANUARY, 1);
        c.add(Calendar.DAY_OF_MONTH, rng.nextInt(2526)); // [19920101, 19981131], aprox.
        int colShippingDate = Integer.parseInt(yyyymmddDateFormat.format(c.getTime()));
        c.add(Calendar.DAY_OF_MONTH, 1 + rng.nextInt(30)); // + [1, 30]
        int returnDate = Integer.parseInt(yyyymmddDateFormat.format(c.getTime()));
        int colReturnFlag = (returnDate <= 19950617)
                ? (rng.nextInt(2) * 2) /* R or A */
                : 1 /* N */;
        int colLineStatus = (colShippingDate <= 19950617) ? 0 /* F */ : 1 /* O */;

        lines[i] = new LineItemRow(colQuantity, colExtendedPrice, colDiscount, colTax,
                colReturnFlag, colLineStatus, colShippingDate);
      }
    }

    System.out.println("Distributing data...");

    int tileHeight = size/tiles;
    for(int tile=0; tile<tiles; tile++){
      LineItemRow[] linesSplit = Arrays.copyOfRange(lines, tile*tileHeight, (tile+1)*tileHeight);

      Range range = Range.create(linesSplit.length);
      DatabaseFilterKernel kernel = new DatabaseFilterKernel(this, range, linesSplit, size);
      kernel.setDevicePreference(preference);
      kernel.setExplicit(true);
      // IMPORTANT: The initial values for the kernel data should *not* be uploaded (put) here,
      //            because Aparapi already does this on its own on the first kernel run
      //            In fact, doing a 'put' here results in the data being uploaded twice!
      //kernel.put(...);
      addKernel(kernel);
    }
  }

}
