package personal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSVParse {
  /**
   * Parses out cols of valueColName from CSV, columns are in original order
   * @param csv
   * @param valueColName list of cols we want to read
   * @return
   * @throws IOException
   */
  public List<List<String>> parseCSV(File csv, List<String> valueColName) throws IOException {
    List<List<String>> listOfLists = new ArrayList<>();
    BufferedReader bufferedReader = new BufferedReader(new FileReader(csv));

    // read first line
    String line = bufferedReader.readLine();
    List<Integer> value_indices = parseFirstLine(line, valueColName);

    while ((line = bufferedReader.readLine()) != null) {
      String[] data = line.split(",");
      if (data.length < 2) {
        continue;
      }

      ArrayList<String> values = new ArrayList<String>();
      for (Integer i : value_indices) {
        if (i == null) {
          values.add(null);
          continue;
        }
        values.add(trim(data[i]));
      }
      listOfLists.add(values);
    }
    return listOfLists;
  }

  private String trim(String s) {
    StringBuilder sb = new StringBuilder(s);
    if (sb.charAt(0) == '=') {
      sb.deleteCharAt(0);
    }
    if (sb.charAt(0) == '"') {
      sb.deleteCharAt(0);
    }
    int endOfString = sb.length() - 1;
    if (sb.charAt(endOfString) == '"') {
      sb.deleteCharAt(endOfString);
    }
    return sb.toString().trim();
  }

  /**
   * return indices of cols in same order as we received
   * @param line first line of CSV
   * @return
   */
  public List<Integer> parseFirstLine(String line, List<String> valueColNames) {
    Integer[] value_indices = new Integer[valueColNames.size()];
    String[] colNames = line.split(",");
    for (int csvColumn = 0; csvColumn < colNames.length; csvColumn++) {
      String colName = colNames[csvColumn];
      for (int i = 0; i < valueColNames.size(); i++) {
        if (colName.toLowerCase().equals(valueColNames.get(i).toLowerCase())) {
          value_indices[i] = csvColumn;
          break;
        }
      }
    }
    return Arrays.asList(value_indices);
  }
}
