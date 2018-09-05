package org.vaadin.jonni.fiddleorchestrator;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import javax.servlet.annotation.WebServlet;

import com.fasterxml.jackson.databind.JsonNode;
import com.guidedsoftware.hyper.HyperException;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.ButtonRenderer;

@Theme("mytheme")
@Push
public class OrchestratorUI extends UI {

	private Grid<JsonNode> containersGrid;
	private Grid<JsonNode> volumesGrid;
	private HyperService hyperService;

	@Override
	protected void init(VaadinRequest vaadinRequest) {
		final VerticalLayout layout = new VerticalLayout();

		hyperService = new HyperService();

		createContainerGrid();
		createVolumesGrid();
		layout.addComponent(new HorizontalLayout(new Button("Create volume", click -> this.createVolume())));
		layout.addComponentsAndExpand(volumesGrid, containersGrid);

		refreshContainerGrid();
		refreshVolumesGrid();

		setContent(layout);
	}

	private void createVolume() {
		String name = "foobar";
		runHyperCallInBackround(() -> {
			hyperService.performCreateVolume(name, result -> getUI().access(() -> {
				Notification.show("Volume created, now initializing!");
				refreshVolumesGrid();
				runHyperCallInBackround(() -> {
					hyperService.initVolume(name, "git://github.com/Wnt/hyper-vaadin-fiddle-stub.git", initResult -> {
						getUI().access(() -> Notification.show("Volume inited!"));
						System.out.println("Success");
						runHyperCallInBackround(
								() -> hyperService.setPermissivePermissionsToVolume(name, permissionSetResult -> {
									getUI().access(() -> {
										Notification.show("Permissions set!");
										refreshContainerGrid();
									});
									System.out.println("permission set success");
								}));
					});
				});
			}));
		});
	}

	private void refreshContainerGrid() {
		runHyperCallInBackround(() -> {
			hyperService.listContainers(result -> {
				getUI().access(() -> {
					Notification.show("Found " + result.size() + " containers");
					// grid.setItems(result.elements());
					List<JsonNode> containers = new ArrayList<>();
					for (final JsonNode container : result) {
						containers.add(container);
					}
					containersGrid.setItems(containers);
				});
			});
		});
	}

	private void refreshVolumesGrid() {
		runHyperCallInBackround(() -> {
			hyperService.listVolumes(result -> {
				getUI().access(() -> {
					Notification.show("Found " + result.size() + " volumes");
					List<JsonNode> volumes = new ArrayList<>();
					for (final JsonNode container : result) {
						volumes.add(container);
					}
					volumesGrid.setItems(volumes);
				});
			});
		});
	}

	private void runHyperCallInBackround(HyperCall call) {
		Thread callThread = new Thread(() -> {
			try {
				call.call();
			} catch (HyperException ex) {
				getUI().access(() -> Notification.show(ex.getMessage(), Type.ERROR_MESSAGE));
				ex.printStackTrace();
			}
		});
		callThread.start();
	}

	private void createVolumesGrid() {
		/**
		 * { "Name": "foobar", "Driver": "hyper", "Mountpoint": "", "Labels": { "size":
		 * "10", "snapshot": "" }, "Scope": "", "CreatedAt": "0001-01-01T00:00:00Z" }
		 */
		volumesGrid = new Grid<JsonNode>("Volumes");
		volumesGrid.addColumn(c -> c.at("/Name").asText()).setCaption("Name");
		// volumesGrid.addColumn(c ->
		// c.at("/Id").asText()).setCaption("Id").setWidth(150);
		volumesGrid.addColumn(c -> c.at("/CreatedAt").asText()).setCaption("CreatedAt");
		volumesGrid.addColumn(c -> c.at("/Mountpoint").asText()).setCaption("Mountpoint");
		volumesGrid.addColumn(c -> null, new ButtonRenderer<JsonNode>(click -> removeVolume(click.getItem()), "Remove"))
				.setCaption("Tools");
		volumesGrid.setSizeFull();
	}

	private void removeVolume(JsonNode volume) {
		runHyperCallInBackround(() -> {
			String volumeName = volume.at("/Name").asText();

			hyperService.removeVolume(volumeName, result -> refreshVolumesGrid());
		});
	}

	private void createContainerGrid() {
		containersGrid = new Grid<JsonNode>("Containers");
		containersGrid.addColumn(c -> {
			StringJoiner sj = new StringJoiner(",");
			c.at("/Names").forEach(n -> sj.add(n.asText()));
			return sj;
		}).setCaption("Name");
		// grid.addColumn(c ->
		//
		// StreamSupport.stream((c.at("/Names")).spliterator(), false).map(n ->
		// n.asText())
		// .collect(Collectors.joining(","))
		//
		// ).setCaption("Name");
		containersGrid.addColumn(c -> c.at("/Id").asText()).setCaption("Id").setWidth(150);
		containersGrid.addColumn(c -> c.at("/Image").asText()).setCaption("Image");
		containersGrid.addColumn(c -> c.at("/Command").asText()).setCaption("Command");
		containersGrid.addColumn(c -> c.at("/State").asText()).setCaption("State");
		containersGrid.addColumn(c -> c.at("/Status").asText()).setCaption("Status");
		containersGrid.addColumn(c -> null, new ButtonRenderer<JsonNode>(click -> removeContainer(click.getItem()), "Remove"))
		.setCaption("Tools");
		containersGrid.setSizeFull();
	}

	private void removeContainer(JsonNode item) {
		runHyperCallInBackround(() -> {
			JsonNode result = hyperService.removeContainer(item.at("/Id").asText());
			getUI().access(() -> {
				Notification.show("Removed!");
				refreshContainerGrid();
			});
		});
	}

	@WebServlet(urlPatterns = "/*", name = "OrchestratorUIServlet", asyncSupported = true)
	@VaadinServletConfiguration(ui = OrchestratorUI.class, productionMode = false)
	public static class OrchestratorUIServlet extends VaadinServlet {
	}
}
