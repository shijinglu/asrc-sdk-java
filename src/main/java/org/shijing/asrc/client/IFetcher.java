package org.shijing.asrc.client;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/** Interface that send request to remote configurations service and fetches configuration. */
public interface IFetcher {
    CompletableFuture<Map<String, IData>> fetch(Map<String, IData> context);
}
