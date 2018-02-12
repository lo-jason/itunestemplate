import org.junit.Assert;
import org.junit.Test;
import personal.CSVParse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class test {
  @Test
  public void testCSVParse() {
    CSVParse csvParse = new CSVParse();
    List<Integer> ints = csvParse.parseFirstLine("a,b,c,D", Arrays.asList(new String[] {"d", "b"}));
    Assert.assertEquals(ints.get(0), Integer.valueOf(3));
    Assert.assertEquals(ints.get(1), Integer.valueOf(1));

    ints = csvParse.parseFirstLine("a,b,c,D", Arrays.asList(new String[] {"E", "b"}));
    Assert.assertEquals(ints.get(0), null);
    Assert.assertEquals(ints.get(1), Integer.valueOf(1));

    ints = csvParse.parseFirstLine("a,b,c,D", Arrays.asList(new String[] {"merch", "b"}));
    Assert.assertEquals(ints.get(0), null);
    Assert.assertEquals(ints.get(1), Integer.valueOf(1));
  }
}
