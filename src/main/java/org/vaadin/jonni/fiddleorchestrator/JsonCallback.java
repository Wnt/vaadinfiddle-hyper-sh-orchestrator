package org.vaadin.jonni.fiddleorchestrator;

import com.fasterxml.jackson.databind.JsonNode;

@FunctionalInterface
public interface JsonCallback {
	public void result(JsonNode result);
}
