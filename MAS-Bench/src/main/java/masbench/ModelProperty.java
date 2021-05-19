package masbench;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

public class ModelProperty {
  private static final String DATASET = "Dataset";
  private static final String PROPERTIES_FILE = "model.properties";

  String name;
  String mapName;
  int numberOfPeople;
  int gpsSeparating;
  Path modelPath;
  /*
     int naiveSize;
     int rationalSize;
     int rubySize;
     */

  public ModelProperty(String name) throws IOException {
    this.name = name;
    modelPath = MasBench.getResourcesDirectoryPath().resolve(DATASET).resolve(name);
    Path propertiesPath = modelPath.resolve(PROPERTIES_FILE);
    Properties properties = new Properties();
    properties.load(new FileInputStream(propertiesPath.toFile()));
    mapName = properties.getProperty("map");
    numberOfPeople = Integer.parseInt(properties.getProperty("population"));
    gpsSeparating = Integer.parseInt(properties.getProperty("gps"));
    /*
       naiveSize = Integer.parseInt(properties.getProperty("naive"));
       rationalSize = Integer.parseInt(properties.getProperty("rational"));
       rubySize = Integer.parseInt(properties.getProperty("ruby"));
       */
  }

  public String getName() {
    return name;
  }

  public String getMapName() {
    return mapName;
  }

  public int getNumberOfPeople() {
    return numberOfPeople;
  }

  public int getGpsSeparating() {
    return gpsSeparating;
  }

  public Path getModelPath() {
    return modelPath;
  }

  /*
     public int getNaiveSize() {
     return naiveSize;
     }

     public int getRationalSize() {
     return rationalSize;
     }

     public int getRubySize() {
     return rubySize;
     }
     */
}
