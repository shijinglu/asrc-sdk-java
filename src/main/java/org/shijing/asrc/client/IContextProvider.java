package org.shijing.asrc.client;

import org.shijing.asrc.client.model.IData;

import java.util.Map;

/** Protocol to get context. */
public interface IContextProvider {

    Map<String, IData> getContext();
}
