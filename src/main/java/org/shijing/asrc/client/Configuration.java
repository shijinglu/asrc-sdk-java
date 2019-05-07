package org.shijing.asrc.client;

import java.util.Set;

/** Use this class to setup the client */
public class Configuration {
    private String namespace;

    /** If whitelist is defined, only send context that are whitelisted. */
    private Set<String> whitelistKeys;

    /** All keys in the blacklist should be filtered out of the context */
    private Set<String> blacklistKeys;
}
