package fr.dynamo;

import fr.dynamo.threading.DynamoKernel;

public interface ThreadFinishedNotifier {
  public void notifyListener(DynamoKernel kernel);
}
