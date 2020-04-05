package ru.sin666.json.proxy.enricher;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import ru.sin666.json.proxy.enricher.dto.Person;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.minidev.json.parser.JSONParser.DEFAULT_PERMISSIVE_MODE;

@RestController
public class DemoController {
    private static final int ELEMENT_COUNT = 20;
    private static final int MAX_AGE = 50;
    private static final String[] names = new String[]{"Sergey", "Aglaya", "Eugenia", "Valeriy", "Natalia", "Marina", "Elena", "Karina", "Ulia"};
    private static final AtomicInteger counter = new AtomicInteger(0);

    /**
     * Endpoint to generate collection of person
     * @return
     */
    @GetMapping("/person/")
    public List<Person> getPerson() {
        return IntStream.range(0, ELEMENT_COUNT).boxed().map(i -> generatePerson(names, MAX_AGE)).collect(Collectors.toList());
    }

    /**
     * Gets the result of the first endpoint and enrich it with metadata
     * @return enriched collection of person
     * @throws ParseException
     */

    @GetMapping("/cheсked_person/")
    public String getCheckedPerson() throws ParseException {
        RestTemplate restTemplate = new RestTemplate();
        String fooResourceUrl = "http://localhost:8080/person/";
        ResponseEntity<String> response = restTemplate.getForEntity(fooResourceUrl, String.class);
        return enrich(response.getBody());
    }

    private String enrich(String json) throws ParseException {
        Function<Function<Integer, Boolean>, Function<JSONObject, JSONObject>> addCheckedFactory = metaDataSupplyer -> jsonChildObject -> {
            Integer id = (Integer) jsonChildObject.get("id");
            jsonChildObject.appendField("isChecked", metaDataSupplyer.apply(id));
            return jsonChildObject;
        };

        JSONArray jsonArray = (JSONArray) (new JSONParser(DEFAULT_PERMISSIVE_MODE)).parse(json);
        Set<String> ids = jsonArray.stream().map(obj -> (JSONObject) obj).map(e -> e.getAsString("id")).collect(Collectors.toSet());
        Function<JSONObject, JSONObject> addChecked = addCheckedFactory.apply(getMetadataSupplier(ids, ""));

        jsonArray.stream().map(obj -> (JSONObject) obj).map(addChecked).count();


        return jsonArray.toString();
    }


    /**
     * @param ids       The set of ids currently processed records. In order to fetch the metadata from DB
     * @param contextId The Identifier of session or certaian operation
     * @return Function that returns metadata by Id
     */
    private Function<Integer, Boolean> getMetadataSupplier(Set<?> ids, String contextId) {
        return i -> i % 2 == 0;
    }


    private static Person generatePerson(String[] names, int maxAge) {
        String name = names[(int) Math.floor(Math.random() * names.length)];
        int age = (int) Math.floor(Math.random() * maxAge);
        return new Person(counter.addAndGet(1), name, age);
    }

}
