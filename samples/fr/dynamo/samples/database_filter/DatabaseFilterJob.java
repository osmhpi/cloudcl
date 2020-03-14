package fr.dynamo.samples.database_filter;

import java.util.Arrays;
import java.util.Random;
import java.io.*;

import com.amd.aparapi.Range;

import fr.dynamo.DevicePreference;
import fr.dynamo.ThreadFinishedNotifyable;
import fr.dynamo.threading.DynamoJob;

public class DatabaseFilterJob extends DynamoJob{

  public DatabaseFilterJob(String tpchLineItemFile, int tiles, DevicePreference preference, ThreadFinishedNotifyable notifyable) throws IOException {
    super("DatabaseFilter", notifyable);
    int size;

    System.out.println("Counting lines...");
    try(FileReader       input = new FileReader(tpchLineItemFile);
        LineNumberReader count = new LineNumberReader(input))
    {
      while (count.skip(Long.MAX_VALUE) > 0) { }
      size = count.getLineNumber();
    }

    System.out.println("Reading " + size + " lines...");
    LineItemRow[] lines = new LineItemRow[size];
    try(FileReader       input = new FileReader(tpchLineItemFile);
        BufferedReader   br    = new BufferedReader(input)) {

      int r = 0;
      for (String line = br.readLine(); line != null; line = br.readLine()) {
        String[] lineParts = line.split("\\|");
        int colQuantity = Integer.parseInt(lineParts[4]);
        int colExtendedPrice = (int)(Float.parseFloat(lineParts[5]) * 100);
        int colDiscount = (int)(Float.parseFloat(lineParts[6]) * 100);
        int colTax = (int)(Float.parseFloat(lineParts[7]) * 100);
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
