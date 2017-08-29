import java.lang.reflect.Field;
import java.util.ArrayList;

import org.junit.Test;

public class TestGlobal {
	@Test
	public void main() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		ArrayList<Integer> list = new ArrayList<Integer>(10);
		for (int i = 0; i < 50; i++) {
			// list.ca
			list.add(i);
			
				Field field = ArrayList.class.getDeclaredField("elementData");
				Object[] dataElement = (Object[]) field.get(list);
				System.out.println(String.format("cap: %s, i: %s", dataElement.length, i));

		}
	}
}
