package com.example.demo;

public class Info {
	private String hostname;
	private String timestamp;
	
	public String getHostname() {
		return hostname;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public Info(String hostname, String timestamp) {
		this.hostname = hostname;
		this.timestamp = timestamp;
	}
}
