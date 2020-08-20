
/*const http = require('http');
const fs = require('fs');*/

const localpath = "/tmp/covid-data.csv"; 


function download(url){
	
	 console.log("In js download method with " + url + " " + localpath)
	 request = http.get("http://i3.ytimg.com/vi/J---aiyznGQ/mqdefault.jpg", function(response) {
		
		if(response.statusCode == 200 ){
			file = fs.createWriteStream(localpath);
			response.pipe(file);
			return localpath;
		}
  		
		request.setTimeout(12000, function () {
        request.abort();
    });

	});
	
	return "";
}