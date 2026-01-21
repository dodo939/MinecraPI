package com.dodo939.minecraPI;

import java.util.List;

public class MyConfig {
    String host;
    int port;
    String secret_key;
    int timestamp_tolerance;
    boolean enable_player_auth;
    int max_player_per_ip;
    List<String> ip_limit_message;
    List<String> notice_message;
    List<String> error_message;
}
