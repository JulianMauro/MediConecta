package com.mediconecta.web;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PingController {

	@GetMapping("/ping")
	public Map<String, Object> ping() {
		return Map.of("status", "ok", "timestamp", Instant.now().toString());
	}
}
