package com.example.demo.exception;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.core.util.Context;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.azure.storage.file.datalake.DataLakeDirectoryClient;
import com.azure.storage.file.datalake.DataLakeFileClient;
import com.azure.storage.file.datalake.DataLakeFileSystemClient;
import com.azure.storage.file.datalake.DataLakeServiceClient;
import com.azure.storage.file.datalake.DataLakeServiceClientBuilder;
import com.azure.storage.file.datalake.models.DataLakeRequestConditions;
import com.azure.storage.file.datalake.models.DownloadRetryOptions;
import com.azure.storage.file.datalake.models.FileRange;
import com.azure.storage.file.datalake.models.PathHttpHeaders;
import com.azure.storage.file.datalake.models.PathInfo;

public class StoreExceptionToBlob {

	public static void storingExceptionInArchiveLocation(String exceptionMessage, String moduleName, UUID uuid) {
		DataLakeServiceClient getDataLakeServiceClient = GetDataLakeServiceClient();

		DataLakeFileSystemClient fileSystemClient = getDataLakeServiceClient
				.getFileSystemClient("woh-alg-pms-datalake-dev");
		
	ZonedDateTime currentInstant = ZonedDateTime.now();
	int year = currentInstant.getYear();
	int month = currentInstant.getMonthValue();
	int day = currentInstant.getDayOfMonth();
	int hour = currentInstant.getHour();
	int minute = currentInstant.getMinute();
	int second = currentInstant.getSecond();
	String fileName = "AMR_"+moduleName+"_"+year+"-"+month+"-"+day+".json";
	
		DataLakeDirectoryClient subdirectoryClient = fileSystemClient.getDirectoryClient("RAW")
				                                    .getSubdirectoryClient("TCA-API")
				                                    .getSubdirectoryClient("PipelineErrors")
				                                    .getSubdirectoryClient(moduleName)
				                                    .getSubdirectoryClient("Archive")
				                                    .getSubdirectoryClient("Archive_"+year+"-"+month+"-"+day)
				                                    .getSubdirectoryClient("Archive_"+year+"-"+month+"-"+day+"_"+hour+"-"+minute+"-"+second)
				                                    .getSubdirectoryClient(uuid.toString());
		DataLakeFileClient fileClient = subdirectoryClient.createFile(fileName, true);
		fileClient.append(BinaryData.fromString(exceptionMessage), 0);
		writeToDataLakeBlobStorageWithResponse(exceptionMessage.length(),fileClient);
	}
	
	private static DataLakeServiceClient GetDataLakeServiceClient (){

	    StorageSharedKeyCredential sharedKeyCredential =
	        new StorageSharedKeyCredential("wohalgpmsdldevsa", "iSh4zGYXanmJdEhCSv/Qg1h+GF37rsfZAwIzzo0nByAgg6itXlDVQHFVe2gf5vK+3l4eFvtPWaVNj2P4f0wQow==");

	    DataLakeServiceClientBuilder builder = new DataLakeServiceClientBuilder();

	    builder.credential(sharedKeyCredential);
	    builder.endpoint("https://" + "wohalgpmsdldevsa" + ".dfs.core.windows.net");

	    return builder.buildClient();
	}
	
	private static boolean writeToDataLakeBlobStorageWithResponse(int i, DataLakeFileClient fileClient) {
		FileRange range = new FileRange(1024, 2048L);
		 DownloadRetryOptions options = new DownloadRetryOptions().setMaxRetryRequests(5);
		 byte[] contentMd5 = new byte[0]; // Replace with valid md5
		 boolean retainUncommittedData = false;
		 boolean close = false;
		 PathHttpHeaders httpHeaders = new PathHttpHeaders()
		     .setContentLanguage("en-US")
		     .setContentType("binary");
		 DataLakeRequestConditions requestConditions = new DataLakeRequestConditions();


		 Response<PathInfo> response = fileClient.flushWithResponse(i, retainUncommittedData, close, httpHeaders,
			     requestConditions, null, Context.NONE);
		 if(response.getStatusCode() == 200) {
			 System.out.printf("Flush of Error Message completed on path: %s with status %d%n", fileClient.getFilePath(),response.getStatusCode());
			 return true;
		 } else {
			 System.out.printf("Flush of Error Message not completed on path: %s with status %d%n", fileClient.getFilePath(),response.getStatusCode());
			 
			 return false;
		 }
	}

	public static void updateToLatestFolder(String exceptionMessage) {
		DataLakeServiceClient getDataLakeServiceClient = GetDataLakeServiceClient();

		DataLakeFileSystemClient fileSystemClient = getDataLakeServiceClient
				.getFileSystemClient("woh-alg-pms-datalake-dev");
		
		DataLakeDirectoryClient subdirectoryClient = fileSystemClient.getDirectoryClient("RAW")
                .getSubdirectoryClient("TCA-API")
                .getSubdirectoryClient("PipelineErrors")
                .getSubdirectoryClient("Latest");
		
		DataLakeFileClient fileClient = subdirectoryClient.createFile("LatestException.json", true);
		fileClient.append(BinaryData.fromString(exceptionMessage), 0);
		writeToDataLakeBlobStorageWithResponse(exceptionMessage.length(),fileClient);
	}
}
