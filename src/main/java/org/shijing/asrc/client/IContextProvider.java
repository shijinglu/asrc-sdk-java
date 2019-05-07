package org.shijing.asrc.client;

import java.util.Map;

/** Protocol to get context. */
public interface IContextProvider {

    Map<String, IData> getContext();
}
