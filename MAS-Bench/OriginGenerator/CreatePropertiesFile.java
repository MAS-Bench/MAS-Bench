import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CreatePropertiesFile {
	static String outputJsonFilePath = "output.json";
	static String fileName = "FS1-1";
	static String signalName = "FS1";
	static String filepath = "FS";
	static String mapsize = "Small";
	
	public static void main(String[] args) {
		outputJsonFilePath = args[0];
		fileName = args[1];
		signalName = args[2];
		filepath = args[3];
		mapsize = args[4];
		writeCsv();
	}
	static void writeCsv() {
		try{
			FileWriter fw = new FileWriter(outputJsonFilePath, false);
			PrintWriter pw = new PrintWriter(new BufferedWriter(fw));
			pw.println("{\r\n" + 
					"  \"map_file\":\""+filepath+"MOJI-"+mapsize+".xml\",\r\n" + 
					"  \"link_appearance_file\":\""+filepath+"Appearance_Link.json\",\r\n" + 
					"  \"node_appearance_file\":\""+filepath+"Appearance_Node.json\",\r\n" + 
					"  \"polygon_appearance_file\":\""+filepath+"Appearance_Polygon.json\",\r\n" + 
					"  \"generation_file\":\"gen.json\",\r\n" + 
					"  \"scenario_file\":\""+filepath+"../Dataset/"+signalName+"/Scenario_Guidance.json\",\r\n" + 
					"  \"fallback_file\":\""+filepath+"Fallback.json\",\r\n" + 
					"  \"camera_file\":\""+filepath+"Camera.json\",\r\n" + 
					"\r\n" + 
					"  \"show_background_map\":false,\r\n" + 
					"  \"gsi_tile_name\":\"ort\",\r\n" + 
					"  \"gsi_tile_zoom\":17,\r\n" + 
					"\r\n" + 
					"\r\n" + 
					"  \"randseed\":2524,\r\n" + 
					"  \"exit_count\":0,\r\n" + 
					"  \"all_agent_speed_zero_break\":true,\r\n" + 
					"\r\n" + 
					"  \"create_log_dirs\":true,\r\n" + 
					"  \"agent_movement_history_file\":\"../log/agent_movement_history.csv\",\r\n" + 
					"  \"individual_pedestrians_log_dir\":\"../log/\",\r\n" + 
					"  \"evacuated_agents_log_file\":\"../log/evacuatedAgent.csv\",\r\n" + 
					"\r\n" + 
					"  \"record_simulation_screen\":false,\r\n" + 
					"  \"screenshot_dir\": \"../log/"+fileName+"/screenshots\",\r\n" + 
					"  \"clear_screenshot_dir\":true,\r\n" + 
					"  \"screenshot_image_type\":\"png\",\r\n" + 
					"\r\n" + 
					"  \"agent_size\":3.0,\r\n" + 
					"  \"zoom\":1.6,\r\n" + 
					"  \"show_3D_polygon\":true,\r\n" + 
					"  \"change_agent_color_depending_on_speed\":true,\r\n" + 
					"  \"show_status\":\"Bottom\",\r\n" + 
					"  \"show_logo\":false,\r\n" + 
					"  \"exit_with_simulation_finished\":true,\r\n" + 
					"  \"simulation_window_open\":false,\r\n" + 
					"  \"auto_simulation_start\":true,\r\n" + 
					"\r\n" + 
					"  \"use_ruby\": true,\r\n" + 
					"  \"ruby_load_path\": \""+filepath+"\",\r\n" + 
					"  \"ruby_simulation_wrapper_class\":\"GateOperation\",\r\n" + 
					"  \"ruby_init_script\":\"\r\n" + 
					"  require 'SampleAgent_Busy'\r\n" + 
					"  require 'SampleAgent_StallIdle'\r\n" + 
					"  require 'SampleAgent_SignalFollow'\r\n" + 
					"  require 'SampleAgent_Route1'\r\n" + 
					"  require 'SampleAgent_Route2'\r\n" + 
					"  require 'SampleAgent_Route3'\r\n" + 
					"  require 'GateOperation'\r\n" + 
					"  $settings = {\r\n" + 
					"    use_gateoperation: false,\r\n" + 
					"    monitor: true,\r\n" + 
					"    gate_node_tag: 'EXIT_STATION_ROOT',\r\n" + 
					"    count_by_entering: true,\r\n" + 
					"    counting_positions: [\r\n" + 
					"      {\r\n" + 
					"        link_tag: 'GL_R1',\r\n" + 
					"        node_tag: 'EXIT_STATION_ROOT'\r\n" + 
					"      },\r\n" + 
					"      {\r\n" + 
					"        link_tag: 'GL_R2',\r\n" + 
					"        node_tag: 'EXIT_STATION_ROOT'\r\n" + 
					"      },\r\n" + 
					"      {\r\n" + 
					"        link_tag: 'GL_R3',\r\n" + 
					"        node_tag: 'EXIT_STATION_ROOT'\r\n" + 
					"      }\r\n" + 
					"    ],\r\n" + 
					"    delay_time: 60,\r\n" + 
					"  }\r\n" + 
					"  \",\r\n" + 
					"\r\n" + 
					"  \"___\" : null\r\n" + 
					"}");

			pw.close();
		}catch (IOException e) {
			System.out.println(e);
		}
	}
}