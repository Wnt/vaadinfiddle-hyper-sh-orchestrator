package org.vaadin.jonni.fiddleorchestrator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.JsonNode;
import com.guidedsoftware.hyper.Hyper;
import com.guidedsoftware.hyper.HyperException;
import com.guidedsoftware.hyper.Region;
import com.guidedsoftware.hyper.RequestType;

public class HyperService {

	private Hyper hyper;

	public HyperService() {
		String accessKey = System.getProperty("hyper.accessKey");
		String secretKey = System.getProperty("hyper.secretKey");
		hyper = Hyper.getInstance(accessKey, secretKey);
		hyper.setRegion(Region.EU_CENTRAL_1);
	}

	public void performCreateVolume(String name, JsonCallback callback) throws HyperException {
		String body = new JSONObject()

				.put("Name", name)

				.put("Driver", "hyper")

				.put("DriverOpts",

						new JSONObject()

								.put("size", "10")

								.put("snapshot", "")

				).toString(4);
		String command = "/volumes/create";
		RequestType requestType = RequestType.POST;

		System.out.println(requestType + " " + command);
		System.out.println(body);

		callback.result(hyper.performRequest(requestType, command, body));
	}

	public void listVolumes(JsonCallback callback) throws HyperException {
		callback.result(hyper.performRequest(RequestType.GET, "/volumes", null).at("/Volumes"));
	}

	public void listContainers(JsonCallback callback) throws HyperException {
		callback.result(hyper.performRequest(RequestType.GET, "/containers/json?all=1", null));
	}

	public void removeVolume(String volumeName, JsonCallback callback) throws HyperException {
		callback.result(hyper.performRequest(RequestType.DELETE, "/volumes/" + volumeName, null));
	}

	public void initVolume(String name, String source, JsonCallback callback) throws HyperException {
		/**
		 * POST /volumes/initialize HTTP/1.1 Content-Type: application/json
		 * 
		 * { "Volume": [ { "Name": "vol1", "Source":
		 * "git://github.com/Wnt/hyper-vaadin-fiddle-stub.git:fiddleapp-stub-vaadin-8-3-1"
		 * } ] }
		 * 
		 */
		String body = new JSONObject()

				.put("Volume", new JSONArray().put(new JSONObject()

						.put("Name", name)

						.put("Source", source)

				)

				).toString(4);
		String command = "/volumes/initialize";
		RequestType requestType = RequestType.POST;

		System.out.println(requestType + " " + command);
		System.out.println(body);

		callback.result(hyper.performRequest(requestType, command, body));

	}

	public void setPermissivePermissionsToVolume(String name, JsonCallback callback) throws HyperException {
		HyperCall createContainer = () -> {
			String body = new JSONObject()

					.put("Image", "alpine")

					.put("Cmd", new JSONArray()

							.put("sh").put("-c")
							.put("find /fiddleapp -type f -exec chmod 0666 {} \\; ; find /fiddleapp -type d -exec chmod 0777 {} \\;")

					)

					// .put("Cmd", new JSONArray()
					//
					// .put("date")
					//
					// )

					.put("HostConfig", new JSONObject()

							.put("Binds", new JSONArray().put(name + ":/fiddleapp")

					))

					.put("Labels", new JSONObject()

							.put("sh_hyper_instancetype", "s1")

					)

					.toString(4);
			String command = "/containers/create";
			RequestType requestType = RequestType.POST;

			System.out.println(requestType + " " + command);
			System.out.println(body);

			JsonNode result = hyper.performRequest(requestType, command, body);

			System.out.println(result);

			String containerId = result.at("/Id").asText();

			JsonNode runResult = startContainer(containerId);
			
			removeContainer(containerId);
			
			callback.result(runResult);
		};
		try {
			createContainer.call();
		} catch (HyperException e) {
			if (e.getMessage().contains("404 No such image")) {
				createImage("alpine");
				createContainer.call();
			} else
				throw e;
		}
	}

	public JsonNode removeContainer(String containerId) throws HyperException {

		return hyper.performRequest(RequestType.DELETE, "/containers/" + containerId + "?v=1&force=1",
				null);
	}

	private JsonNode startContainer(String containerId) throws HyperException {
		return hyper.performRequest(RequestType.POST, "/containers/" + containerId + "/start",
				null);

	}

	private JsonNode createImage(String imageName) throws HyperException {
		return hyper.performRequest(RequestType.POST, "/images/create?fromImage=" + imageName, null);
	}
}
