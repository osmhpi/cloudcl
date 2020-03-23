
package fr.dynamo.samples.database_filter;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;
import java.io.*;
import java.util.stream.IntStream;

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

    final int size;

    if (useFile) {
      System.out.println("Counting lines...");
      try (FileReader input = new FileReader(sizeOrTpchLineItemFile);
           LineNumberReader count = new LineNumberReader(input)) {
        //noinspection StatementWithEmptyBody
        while (count.skip(Long.MAX_VALUE) > 0) {
        }
        size = count.getLineNumber();
      }
    } else {
      size = Integer.parseInt(sizeOrTpchLineItemFile);
    }

    int[] colQuantity = new int[size];
    int[] colExtendedPrice = new int[size];
    int[] colDiscount = new int[size];
    int[] colTax = new int[size];
    int[] colReturnFlag = new int[size];
    int[] colLineStatus = new int[size];
    int[] colShippingDate = new int[size];

    if (useFile) {
      System.out.println("Reading " + size + " lines...");
      try (FileReader input = new FileReader(sizeOrTpchLineItemFile);
           BufferedReader br = new BufferedReader(input)) {
        int i = 0;
        for (String line = br.readLine(); line != null; line = br.readLine()) {
          String[] lineParts = line.split("\\|");
          colQuantity[i] = Integer.parseInt(lineParts[4]);
          colExtendedPrice[i] = (int) (Float.parseFloat(lineParts[5]) * 100);
          colDiscount[i] = (int) (Float.parseFloat(lineParts[6]) * 100);
          colTax[i] = (int) (Float.parseFloat(lineParts[7]) * 100);
          colReturnFlag[i] = lineParts[8].equals("A") ? 0 :
                  lineParts[8].equals("N") ? 1 :
                          lineParts[8].equals("R") ? 2 :
                                  -1;
          colLineStatus[i] = lineParts[9].equals("F") ? 0 :
                  lineParts[9].equals("O") ? 1 :
                          -1;
          colShippingDate[i] = Integer.parseInt(lineParts[10].replace("-", ""));
          i++;
        }
      }
    } else {
      System.out.println("Generating " + size + " lines...");

      int NUM_THREADS = Runtime.getRuntime().availableProcessors();
      IntStream.range(0, NUM_THREADS).parallel().forEach(ps -> {
        Random random = new Random(12345 + ps);
        Calendar c = Calendar.getInstance();

        for (int i = ps; i < size; i += NUM_THREADS) {
          colQuantity[i] = 1 + random.nextInt(50); // [1, 50]
          colDiscount[i] = random.nextInt(11); // [0, 10]
          colTax[i] = random.nextInt(9); // [0, 8]
          int unitPrice = 90000 + random.nextInt(100001); // [90000,190000], aprox.
          colExtendedPrice[i] = unitPrice * colQuantity[i];
          c.set(1992, Calendar.JANUARY, 1);
          c.add(Calendar.DAY_OF_MONTH, random.nextInt(2526)); // [19920101, 19981131], aprox.
          colShippingDate[i] = calendarToYYYYMMDDInteger(c);
          c.add(Calendar.DAY_OF_MONTH, 1 + random.nextInt(30)); // + [1, 30]
          int returnDate = calendarToYYYYMMDDInteger(c);
          colReturnFlag[i] = (returnDate <= 19950617)
                  ? (random.nextInt(2) * 2) /* R or A */
                  : 1 /* N */;
          colLineStatus[i] = (colShippingDate[i] <= 19950617) ? 0 /* F */ : 1 /* O */;

        }
      });
    }

    System.out.println("Distributing data...");

    int tileHeight = size/tiles;
    for(int tile=0; tile<tiles; tile++){
      int splitStart = tile*tileHeight, splitEnd = (tile+1)*tileHeight;
      int[] colQuantitySplit = Arrays.copyOfRange(colQuantity, splitStart, splitEnd);
      int[] colExtendedPriceSplit = Arrays.copyOfRange(colExtendedPrice, splitStart, splitEnd);
      int[] colDiscountSplit = Arrays.copyOfRange(colDiscount, splitStart, splitEnd);
      int[] colTaxSplit = Arrays.copyOfRange(colTax, splitStart, splitEnd);
      int[] colReturnFlagSplit = Arrays.copyOfRange(colReturnFlag, splitStart, splitEnd);
      int[] colLineStatusSplit = Arrays.copyOfRange(colLineStatus, splitStart, splitEnd);
      int[] colShippingDateSplit = Arrays.copyOfRange(colShippingDate, splitStart, splitEnd);

      Range range = Range.create(splitEnd - splitStart, 256);
      DatabaseFilterKernel kernel = new DatabaseFilterKernel(this, range, colQuantitySplit, colExtendedPriceSplit,
              colDiscountSplit, colTaxSplit, colReturnFlagSplit, colLineStatusSplit, colShippingDateSplit);
      kernel.setDevicePreference(preference);
      kernel.setExplicit(true);
      // IMPORTANT: The initial values for the kernel data should *not* be uploaded (put) here,
      //            because Aparapi already does this on its own on the first kernel run
      //            In fact, doing a 'put' here results in the data being uploaded twice!
      //kernel.put(...);
      addKernel(kernel);
    }
  }

  private static int calendarToYYYYMMDDInteger(Calendar c) {
    return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH);
  }

}
