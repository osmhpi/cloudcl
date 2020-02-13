package fr.dynamo;

import fr.dynamo.threading.DynamoThread;

public interface ThreadFinishedNotifyable {
  void notifyListener(DynamoThread thread);
}
