
package org.graalvm.demo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.graalvm.demo.CovidDtoTable.CovidDto;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

/**
 * A simple JAX-RS resource to greet you. Examples:
 *
 * Get default Hospitalization graph (Yvelines) : curl -X GET
 * http://localhost:8080/covid-19/fr/
 *
 * Get Hospitalization graph for PARIS(75) : curl -X GET
 * http://localhost:8080//covid-19/fr/75
 *
 * Change greeting curl -X PUT -H "Content-Type: application/json" -d
 * '{"greeting" : "Howdy"}' http://localhost:8080/greet/greeting
 *
 * The message is returned as a JSON object.
 */
@Path("/covid-19/fr/")
@RequestScoped
public class CovidResource {

	private static final String YVLINES_DEPARTMENT = "78";
	private static Logger logger = Logger.getLogger(CovidResource.class.getName());

	private Source rSource;
	private final String scriptUrl;
	private Context polyglot;
	private String csvLocalFilePath;
	private String csvDataRemoteSource;
	private String llvmScriptUrl;
	private String pythonScript;
	private String jsScript;

	Function<String, String> getDepartmentNameByIdFunc;
	Function<String, String> downloadCsvDataFunc;

	@Inject
	public CovidResource(@ConfigProperty(name = "app.covid.rscript.url") String scriptUrl,
			@ConfigProperty(name = "app.covid.data.download.csvfullpath") String csvLocalFilePath,
			@ConfigProperty(name = "app.covid.data.download.source") String csvDataRemoteSource,
			@ConfigProperty(name = "app.covid.cscript.url") String llvmScriptUrl,
			@ConfigProperty(name = "app.covid.pyscript") String pythonScript,
			@ConfigProperty(name = "app.covid.jsscript") String jsScript) {
		this.scriptUrl = scriptUrl;
		this.llvmScriptUrl = llvmScriptUrl;
		this.csvLocalFilePath = csvLocalFilePath;
		this.csvDataRemoteSource = csvDataRemoteSource;
		this.pythonScript = pythonScript;
		this.jsScript = jsScript;

		try {
			this.rSource = Source.newBuilder("R", new URL(scriptUrl)).name("covid.R").build();
			this.polyglot = Context.newBuilder().allowAllAccess(true).build();

			this.getDepartmentNameByIdFunc = loadPythonScript();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Path("/cdownload")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response llvmDownloadCsvData() {

		try {
			Source source = Source.newBuilder("llvm", new URL(llvmScriptUrl)).build();
			Value cpart = polyglot.eval(source);
			cpart.execute();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		logger.info(String.format("Downloading file %s to %s", csvDataRemoteSource, csvLocalFilePath));
		return Response.ok().build();
	}

	@Path("/department")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response downloadCsvData2() {

		try {

			String dname = getDepartmentNameByIdFunc.apply("12");
			logger.info(String.format("Departement 12=%s", dname));

			return Response.ok().build();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Response.status(Status.INTERNAL_SERVER_ERROR).build();

	}

	@Path("/download")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response downloadCsvData() {

		try {

			BufferedInputStream inputStream = new BufferedInputStream(new URL(csvDataRemoteSource).openStream());
			FileOutputStream fileOS = new FileOutputStream(csvLocalFilePath);
			byte data[] = new byte[1024];
			int byteContent;

			while ((byteContent = inputStream.read(data, 0, 1024)) != -1) {
				fileOS.write(data, 0, byteContent);
			}
			inputStream.close();
			fileOS.close();
			logger.info(String.format("localpath =%S", csvLocalFilePath));

			return Response.ok().build();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Response.status(Status.INTERNAL_SERVER_ERROR).build();

	}

	/**
	 * Load the python script that provide a function to retreive department names.
	 * 
	 * @return
	 * @throws IOException
	 */
	private Function<String, String> loadPythonScript() throws IOException {
		Source source = Source.newBuilder("python", new File(pythonScript)).build();
		Value pyPart = polyglot.eval(source);
		Function<String, String> getDepartmentNameByIdFunc = pyPart.getContext().getPolyglotBindings()
				.getMember("getDepartmentNameById").as(Function.class);
		return getDepartmentNameByIdFunc;
	}

	private Function<String, String> loadJScript() throws IOException {
		Source source = Source.newBuilder("js", new File(jsScript)).build();
		Value jsPart = polyglot.eval(source);
		Function<String, String> downloadCsvDataFunc = jsPart.getContext().getPolyglotBindings().getMember("download")
				.as(Function.class);
		return downloadCsvDataFunc;
	}

	@Path("/{departmentId}")
	@GET
	@Produces({ "image/svg+xml" })
	public Response getCovidHospitalisationGraphic(@PathParam("departmentId") String departmentId) {

		CovidDto[] datas = { new CovidDto(departmentId, csvLocalFilePath) };
		CovidDtoTable dataTable = new CovidDtoTable(datas);
		Function<CovidDtoTable, String> rplotFunc = polyglot.eval(rSource).as(Function.class);
		String svgData = rplotFunc.apply(dataTable);

		return Response.ok(svgData).build();

	}

	@Path("/")
	@GET
	@Produces({ "image/svg+xml" })
	public Response getDefaultCovidHospitalisationGraphic() {
		return getCovidHospitalisationGraphic(YVLINES_DEPARTMENT);
	}

}
