package org.opendatamesh.cli.extensions;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.opendatamesh.cli.utils.CliFileUtils;
import org.opendatamesh.dpds.model.core.StandardDefinitionDPDS;
import org.opendatamesh.dpds.model.definitions.DefinitionReferenceDPDS;
import org.opendatamesh.dpds.model.interfaces.PortDPDS;
import org.opendatamesh.dpds.model.interfaces.PromisesDPDS;
import org.opendatamesh.dpds.utils.ObjectMapperFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.json.JSONObject;

import net.minidev.json.JSONArray;

public class JDBCImportTool implements ImportTool {

    public JDBCImportTool() {}

    @Override
    public String from() {
        return "jdbc";
    }

    @Override
    public String to() {
        return "port";
    }

    @Override
    public Object importElement(File descriptorFile, Object target, Map outParamMap) throws IOException {
        if ((target instanceof PortDPDS) == false)
            throw new IOException("Impossible to import to [" + target.getClass().getName() + "]");

        PortDPDS port = (PortDPDS) target;

        String portName = (String) outParamMap.get("name");
        if (portName == null) {
            portName = UUID.randomUUID().toString();
            outParamMap.put("name", portName);
        }

        String portRef = "ports/output/" + portName + "/port.json";
        port.setRef(portRef);
        port.setRawContent(" {\"$ref\": \"" + portRef + "\"}");

        File portFile = new File(descriptorFile.getParentFile(), portRef);
        File portFoder = portFile.getParentFile();

        ObjectMapper mapper = ObjectMapperFactory.JSON_MAPPER;

        PortDPDS portDef = importOutputPort(outParamMap);
        String portDefContent = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(portDef);
        CliFileUtils.writeFile(portFoder.getPath(), portFile.getName(), portDefContent);

        JSONObject api = new JSONObject();
        api.appendField("datastoreapi", "1.0.0");
        JSONObject schema = new JSONObject();
        if (outParamMap.containsKey("database"))
            schema.appendField("databaseName", outParamMap.get("database"));
        if (outParamMap.containsKey("schema"))
            schema.appendField("schemaName", outParamMap.get("schema"));
        schema.appendField("tables", new JSONArray());
        api.appendField("schema", schema);

        CliFileUtils.writeFile(portFoder.getPath(), "api.json",
                mapper.writerWithDefaultPrettyPrinter().writeValueAsString(api));

        return port;
    }

    PortDPDS importOutputPort(Map<String, String> params) {
        PortDPDS port = new PortDPDS();

        port.setName(params.get("name"));
        port.setVersion("1.0.0");

        PromisesDPDS promises = new PromisesDPDS();
        StandardDefinitionDPDS apiStdDef = new StandardDefinitionDPDS();
        apiStdDef.setSpecification("datastoreapi");
        apiStdDef.setSpecification("1.0.0");
        DefinitionReferenceDPDS deinition = new DefinitionReferenceDPDS();
        deinition.setMediaType("text/json");
        deinition.setRef("api.json");
        apiStdDef.setDefinition(deinition);
        promises.setApi(apiStdDef);
        port.setPromises(promises);

        return port;
    }

}
