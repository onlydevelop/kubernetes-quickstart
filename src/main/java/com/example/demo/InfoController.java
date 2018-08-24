package com.example.demo;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InfoController {
	
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
	
	@RequestMapping("/")
	public Info getInfo() throws UnknownHostException {
		return new Info(
				InetAddress.getLocalHost().getHostName(),
				sdf.format(new Timestamp(System.currentTimeMillis()))
				);
	}
}
