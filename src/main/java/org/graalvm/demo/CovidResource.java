
package org.graalvm.demo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
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
 * Download covid Dataset from French Heath Agency
 * http://localhost:8080/covid-19/download
 * 
 * Check the download status
 *  curl -X GET -I curl localhost:8080/covid-19/fr/download/status 
 * HTTP 200 file download and is present
 * HTTP 500 download failed.
 * 
 *
 ** Get department name by Id ( python call)
 * http://localhost:8080/covid-19/fr/department/{departmentId}
 * 
 * Get Hospitalization graph for a department : curl -X GET
 * 
 * http://localhost:8080/covid-19/fr/trends/{departmentId}
 * 
 * Get Hospitalization graph for PARIS(departmentID=75) curl -X GET
 * http://localhost:8080/covid-19/fr/trends/75
 *
 *
 * Get default Hospitalization graph (departmentId=78) : curl -X GET
 * http://localhost:8080/covid-19/fr/
 * 
 * 
 */
@Path("/covid-19/fr/")
@RequestScoped
public class CovidResource {

	private static final String YVLINES_DEPARTMENT = "78";
	private static Logger logger = Logger.getLogger(CovidResource.class.getName());

	private Source rSource;
	private String rScriptFile;
	private Context polyglot;
	private String csvLocalFilePath;
	private String csvDataRemoteSource;
	private String llvmScriptFile;
	private String pythonScriptFile;

	Function<String, String> getDepartmentNameByIdFunc;
	Function<String, String> downloadCsvDataFunc;

	@Inject
	public CovidResource(@ConfigProperty(name = "app.covid.rscript") String rScriptUrl,
			@ConfigProperty(name = "app.covid.data.download.csvfullpath") String csvLocalFilePath,
			@ConfigProperty(name = "app.covid.data.download.source") String csvDataRemoteSource,
			@ConfigProperty(name = "app.covid.cscript") String llvmScriptFile,
			@ConfigProperty(name = "app.covid.pyscript") String pythonScriptFile) {
		this.rScriptFile = rScriptUrl;
		this.llvmScriptFile = llvmScriptFile;
		this.csvLocalFilePath = csvLocalFilePath;
		this.csvDataRemoteSource = csvDataRemoteSource;
		this.pythonScriptFile = pythonScriptFile;

		try {
			this.rSource = Source.newBuilder("R", new File(rScriptFile)).build();
			this.polyglot = Context.newBuilder().allowAllAccess(true).build();

			this.getDepartmentNameByIdFunc = loadPythonScript();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Check the local datafile file size using a C function call
	 * @return
	 */
	@Path("/download/status")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response llvmDownloadCsvData() {

		try {
			Source source = Source.newBuilder("llvm", new File(llvmScriptFile)).build();
			Value cpart = polyglot.eval(source);
			int status = cpart.execute(null, csvLocalFilePath).asInt();
			if(status == 0 ) {
				logger.info(String.format("\n[SUCCES] Downloaded file %s to %s", csvDataRemoteSource, csvLocalFilePath));
				return Response.ok().build();
			}
			else {
				logger.log(java.util.logging.Level.SEVERE, String.format("\n[ERROR] while Downloading file %s to %s", csvDataRemoteSource, csvLocalFilePath));
				return Response.serverError().build();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		
		return Response.ok().build();
	}

	@Path("/department/{departmentId}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getDepartmentName(@PathParam("departmentId") String departmentId) {

		try {
			String dname = getDepartmentNameByIdFunc.apply(departmentId);
			logger.info(String.format("Departement 1%s=%s", departmentId, dname));
			return Response.ok(dname).build();

		} catch (Exception e) {
			e.printStackTrace();
		}
		return Response.status(Status.INTERNAL_SERVER_ERROR).build();

	}

	/**
	 * Download the most recent CSV Data file provided by French Health Agency
	 * 
	 * @return
	 */
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
	 * Load the PYTHON script that provide a function to retrieve department names
	 * from their Ids.
	 * 
	 * @return
	 * @throws IOException
	 */
	private Function<String, String> loadPythonScript() throws IOException {
		Source source = Source.newBuilder("python", new File(pythonScriptFile)).build();
		Value pyPart = polyglot.eval(source);
		@SuppressWarnings("unchecked")
		Function<String, String> getDepartmentNameByIdFunc = pyPart.getContext().getPolyglotBindings()
				.getMember("getDepartmentNameById").as(Function.class);
		return getDepartmentNameByIdFunc;
	}

	@Path("/trends/{departmentId}")
	@GET
	@Produces({ "image/svg+xml" })
	public Response getCovidHospitalisationGraphic(@PathParam("departmentId") String departmentId) {

		// Validate departementId formating with Javascript
		// 1=> 01, 2=>02 .... 9=>09

		polyglot.eval("js",
				(" function patchDeptId (deptId) { if(deptId.length==1) {return `0`+deptId } else return deptId } "));

		departmentId = polyglot.getBindings("js").getMember("patchDeptId").execute(departmentId).asString();

		polyglot.eval("js", String.format("print('running with departmentId=%s in JavaScript!')", departmentId));

		// Get the department Name from Python script
		String departmentName = getDepartmentNameByIdFunc.apply(departmentId);

		// Display the covid graph in R for the selected departement

		CovidDto[] datas = { new CovidDto(departmentId, csvLocalFilePath, departmentName) };
		CovidDtoTable dataTable = new CovidDtoTable(datas);
		Function<CovidDtoTable, String> rplotFunc = polyglot.eval(rSource).as(Function.class);
		String svgData = rplotFunc.apply(dataTable);

		return Response.ok(svgData).build();

	}

	/**
	 * Default endpoint: return the graph for department =78
	 * 
	 * @return
	 */
	@Path("/")
	@GET
	@Produces({ "image/svg+xml" })
	public Response getDefaultCovidHospitalisationGraphic() {
		return getCovidHospitalisationGraphic(YVLINES_DEPARTMENT);
	}

}
