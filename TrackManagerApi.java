package com.dawbot.www;

import java.io.BufferedInputStream;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TrackManagerApi {
	
    private static final Logger logger = LoggerFactory.getLogger(TrackManagerApi.class);
	
	public static final String TRACK_ORIGIN_CREATE = "CREATE";
	public static final String TRACK_ORIGIN_REVAMP = "REVAMP";
	
	public static final String CREATE_TRACK_TYPE_GENREMOOD = "GenreMood";
	public static final String CREATE_TRACK_TYPE_TEXTTOMUSIC = "TextToMusic";
	
	public static JSONObject createTrack(HttpServletRequest request, HttpServletResponse response) throws Exception {
		response.setContentType("application/json");
		response.setStatus(200);
		JSONObject jsonObjRes = new JSONObject();
		
		// Get the request body, this is json
		StringBuilder bodyIn = new StringBuilder();
		String line = null;
		BufferedReader reader = request.getReader();
		while ((line = reader.readLine()) != null) {
			bodyIn.append(line);
		}
		String requestBody = bodyIn.toString();
		
		logger.info("REQUEST=BODY============================================================");
		logger.info(requestBody);
		logger.info("============================================================");
		
		// Extract the info from the json
		
		JSONObject jsonObj;
		try {
			jsonObj = new JSONObject(requestBody);
		} catch(Exception e) {
			logger.error("Invalid JSON", e);
			return createErrorResponse(response, 400, false, false, "Error invalid JSON", 101, null);
		}
		
		// Make sure the user is logged in
		User user = new User(request.getSession());
		logger.info("********** In TrackManagerAPI - user email - " + user.getEmail());
		logger.info("********** In TrackManagerAPI - user logged In - " + user.isLoggedIn);
		if (!user.isLoggedIn()) {
			logger.error("User not logged in");
			return createErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, true, false, "Invalid User", 102, null);
		}
		
		// If this user does not have a Plan, send them to Sign Out
		if (!Util.doesUsersHavePlan(user.getFirebase_id())) {
			logger.error("User doesn't have plan");
			return createErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, true, false, "Invalid User", 102, null);
		}

		// Get the users object
		Users users = DatastoreManager.getUsers(user.getFirebase_id());
		
		String userPlan = users.getPlan();
		String playlist = jsonObj.optString("playlist");
		String duration = jsonObj.optString("duration");
		String intensity = jsonObj.optString("intensity");
		String trackformat = jsonObj.optString("trackformat");
		String createType = jsonObj.optString("createtype");
		String textToMusicPrompt = jsonObj.optString("texttomusicprompt");
		String textToMusicLanguage = jsonObj.optString("texttomusiclanguage");
		logger.info("Parsed out playlist: " + playlist);
		logger.info("Parsed out duration: " + duration);
		logger.info("Parsed out intensity: " + intensity);
		logger.info("Parsed out trackformat: " + trackformat);
		logger.info("Parsed out createType: " + createType);
		logger.info("Parsed out textToMusicPrompt: " + textToMusicPrompt);
		logger.info("Parsed out textToMusicLanguage: " + textToMusicLanguage);
		
		
        String userId = jsonObj.optString("uid");
        String accessToken = jsonObj.optString("atk");
        
        System.out.println("In CreateTrackAPI - checking userId passed in - " + userId);
        System.out.println("In CreateTrackAPI - checking accessToken passed in - " + accessToken);
        
        // Make sure we have the proper parameters for each type of createType
        
        if ((createType.equalsIgnoreCase("GenreMood") && Util.isEmpty(playlist)) || (createType.equalsIgnoreCase("TextToMusic") && Util.isEmpty(textToMusicPrompt))) {
			logger.error("CreateTrack - playlist or duration or intensity or trackformat are empty");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters", 103, null);
        }
        
		if (Util.isEmpty(duration) || Util.isEmpty(intensity) || Util.isEmpty(trackformat) || Util.isEmpty(userId) || Util.isEmpty(accessToken)) {
			logger.error("CreateTrack - playlist or duration or intensity or trackformat are empty");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters", 103, null);
		}
		
		if (!areDurationIntensityFormatValid(userPlan, duration, intensity, trackformat)) {
			logger.error("CreateTrack - duration or trackformat are not valid");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters", 103, null);
		}
		
		// If this is Text to Music, make sure it is not longer than 200 characters 
		// and doesn't contain invalid characters
		if (createType.equalsIgnoreCase("TextToMusic")) {
			if (!isTextToMusicPromptValid(textToMusicPrompt, textToMusicLanguage)) {
				logger.error("CreateTrack - textToMusicPrompt is not valid");
				return createErrorResponse(response, 200, false, false, "Error invalid parameters - textToMusic", 106, null);
			}
		}
		

//		Boolean check = FirebaseUtil.isValidToken(userId, accessToken);
//        System.err.println("Token Check - " + check);

//		if (!check) {
//			System.err.println("Error in TrackManagerApi - userId/accessToken not valid");
//			response.setStatus(400);
//			jsonObjRes.put("success", false);
//			jsonObjRes.put("error", "Error invalid parameters");
//			return jsonObjRes;
//		}
		
		// Check the limit of the TempLimitUsers
		if (Util.isTempLimitUserAndOverTheLimit(userId)) {
			logger.error("CreateTrack - no more creates available:  userId - " + userId);
			return createErrorResponse(response, 400, false, false, "No more creates available", 104, null);
		}
		
		String textToMusicEnglish = textToMusicPrompt;
		// If this is a Text To Music and the source language is not English, translate it before passing it in
		if (createType.equalsIgnoreCase("TextToMusic")) {
			if (!textToMusicLanguage.equals("en") && !textToMusicLanguage.equals("n/a")) {
				textToMusicEnglish = TranslateUtil.translateText("",  textToMusicPrompt,  textToMusicLanguage);
				String tempuuid = UUID.randomUUID().toString();
				TranslationUsage translationUsage = new TranslationUsage(tempuuid, userId, textToMusicPrompt, textToMusicLanguage, textToMusicEnglish);
			} 
		}

		logger.info("********** About to call Mybert - textToMusicPrompt - " + textToMusicPrompt + "    textToMusicLanguage - " + textToMusicLanguage + "   textToMusicEnglish - " + textToMusicEnglish);
		
//		SlackUtil.sendMessage("User about to call MubertApi - playlist - " + playlist + "  duration - " + duration + "   IP - " + request.getRemoteAddr());
//		MubertTrack mubertTrack = MubertApiUtil.createTrack(createType, playlist, textToMusicPrompt, duration, intensity, trackformat);
		MubertTrack mubertTrack = MubertApiUtil.createTrack(createType, playlist, textToMusicEnglish, duration, intensity, trackformat);
		
		// If this is TextToMusic and Mubert return object is null, for not put up a message that it is with the TTM prompt
		if (createType.equalsIgnoreCase("TextToMusic")) {
			if (mubertTrack == null) {
				logger.error("CreateTrack - textToMusicPrompt - Mubert return object is null");
				return createErrorResponse(response, 200, false, false, "Error invalid parameters - textToMusic", 107, null);
			}
		}
		

		
// TODO: This is currently throwing an error as the license has expired.  I am hard coding the 1 track below
// TODO: We need to revert some of this once the license is up and running again
		System.out.println("Created task_id - " + mubertTrack.getTask_id());
		System.out.println("Created task_status_code - " + mubertTrack.getTask_status_code());
		System.out.println("Created task_status_text - " + mubertTrack.getTask_status_text());
		System.out.println("Created track - " + mubertTrack.getDownload_link());
		
		boolean trackDone = false;
//		boolean trackDone = true;
		if (mubertTrack.getTask_status_code() == 2) {
			trackDone = true;
		}
		

// Keep on checking to make sure track is done every 3 seconds
// But stop checking after 42 seconds
				
		int trackCheckCount = 0;
		int trackCheckMax = 14;
		boolean trackCheckCountExceeded = false;
		int trackCheckSeconds = 3000;
		while (!trackDone) {
			trackCheckCount++;
			if (trackCheckCount > trackCheckMax) {
				trackCheckCountExceeded = true;
				break;
			}
			boolean checkStatus = MubertApiUtil.isTrackDone(mubertTrack.getTask_id());
			System.out.println("Check Status - " + checkStatus);
			if (checkStatus) {
				trackDone = true;
			}
			Thread.sleep(trackCheckSeconds);
		}

		if (trackCheckCountExceeded) {
			logger.error("********** In TrackManagerAPI - Track Count Check Exceeded");
			return createErrorResponse(response, 200, false, false, "Track Count Check Exceeded", 105, null);
		}

		
//// Temporary Code while Mubert API license is expired to return some track		
////		String tempURLmp3 = "https://static.gcp.mubert.com/backend_content/render/prod/tracks/mp3/e51f7a0a3ac94edd8bde85b6f6565046.mp3";
////		String file = "/Users/mitchmeans/temptestmp3file.mp3";
//		String baseURL = "https://storage.cloud.google.com/mmeans_dawbot_tracks/";
		String baseURL = "https://storage.googleapis.com/mmeans_dawbot_tracks/";
//		String baseURL = "https://storage.cloud.google.com/mmeans_dawbot_tracks_test/";
//		String baseURL = "https://storage.googleapis.com/mmeans_dawbot_tracks_test/";
		String trackURL = baseURL;
		String trackName = "";
		String displayName = "New Track";
		String category = null;
		String group = null;
		String uuid = UUID.randomUUID().toString();
        Integer watermarkFlagValue = 0;

		try {
//			URL url = new URL(tempURLmp3);
			URL url = new URL(mubertTrack.getDownload_link());
	        BufferedInputStream bis = new BufferedInputStream(url.openStream());
//	        FileOutputStream fis = new FileOutputStream(file);
	        ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        byte[] buffer = new byte[1024];
	        int count=0;
	        while((count = bis.read(buffer,0,1024)) != -1)
	        {
//	            fis.write(buffer, 0, count);
	            baos.write(buffer, 0, count);
	        }
//	        fis.close();
	        
	        String projectId = "test-project-311421";
	        String bucketName = "mmeans_dawbot_tracks";
	        category = MubertApiUtil.getCategoryFromPlaylist(playlist);
	        // Because of the 3 level playlist (like 6.4.2) genres we added, this caused some inconsistencies in the nomenclature
	        // The 3rd level Mubert calls a category, but I will be adding it as a group for now.
//	        group = MubertApiUtil.getGroupFromPlaylist(playlist);
	        group = MubertApiUtil.getPlaylistNameFromPlaylist(playlist);
	        LocalDateTime instance = LocalDateTime.now();
//	        DateTimeFormatter formatter	= DateTimeFormatter.ofPattern("yyyy-MM-dd_hh:mm:ss");
//	        DateTimeFormatter formatter	= DateTimeFormatter.ofPattern("yyyy-MM-dd");
//	        String formattedDateString = formatter.format(instance);
//	        trackName = category + "_" + group + "_" + duration + "_" + formattedDateString + "_" + userId.substring(0,8) + ".mp3";
	        
	        String formatExtension = ".mp3";
			if (trackformat.equalsIgnoreCase("hqwav")) {
				formatExtension = ".wav";
			}
	        	
	        trackName = uuid + formatExtension;
	        trackURL = trackURL + trackName;
	        
			if (userPlan.equals(PlanManager.PLAN_FREE)) {
				String watermarkOriginalTrackName = uuid + "-original" + formatExtension;
				StorageUtil.uploadWatermarkOriginalDataToBucket(projectId, bucketName, watermarkOriginalTrackName, baos.toByteArray());
				watermarkTrack(uuid);
				watermarkFlagValue = 1;
			} else {
				StorageUtil.uploadDataToBucket(projectId, bucketName, trackName, baos.toByteArray());
			}
			bis.close();
		} catch (Exception e) {
			logger.error("Process Track - Add Watermark - ",e);
		}
		
		jsonObjRes.put("success", true);
//		jsonObjRes.put("trackURL", mubertTrack.getDownload_link());
		jsonObjRes.put("trackId", uuid);
		jsonObjRes.put("trackURL", trackURL);
		jsonObjRes.put("trackDisplayName", displayName);
		
/*
		UserTracks userTrack = DatastoreManager.getUserTracks(userId);
		if (userTrack == null) {
			userTrack = new UserTracks(userId);
		}
		userTrack.setTrack(baseURL, trackName, category + "_" + group, playlist, duration);
		DatastoreManager.saveUserTrack(userTrack);
*/		

		// Update the track information
		Tracks track = new Tracks(uuid, userId,displayName,trackURL,mubertTrack.getTask_id(),playlist,category,group,"",duration,intensity,trackformat,createType,textToMusicPrompt,textToMusicLanguage, textToMusicEnglish, watermarkFlagValue);
		DatastoreManager.putTrack(track);
		
		// And update the TempLimitUsers if needed
		if (Util.isTempLimitUser(userId)) {
			TempLimitUsers tempLimitUsers = DatastoreManager.getTempLimitUsers(userId);
			tempLimitUsers.setCreateCount(tempLimitUsers.getCreateCount() + 1);
			DatastoreManager.putTempLimitUsers(tempLimitUsers);
		}
		
		// Update the Users totalCount and limit Count
		// For now, make sure they exist and have a plan
		// Mark these as Created and (Saved until they Discard them later)
		if (users != null) {
			if (users.getPlan() != null) {
				users.setTotalTracksCreated(users.getTotalTracksCreated() + 1);
				users.setTotalTracksSaved(users.getTotalTracksSaved() + 1);
				users.setQuotaTracksCreated(users.getQuotaTracksCreated() + 1);
				users.setQuotaTracksSaved(users.getQuotaTracksSaved() + 1);
				users.save();
			}
		}
		
		// Add the quotas to the response
		jsonObjRes.put("quotaTracksCreated", users.getQuotaTracksCreated());

		
//		SqlUtil.getInstance().insertTrack(uuid, userId, trackURL, displayName, playlist, category, group, "", duration);


		
		return jsonObjRes;
		
	}

	public static JSONObject getTracks(HttpServletRequest request, HttpServletResponse response) throws Exception {
		response.setContentType("application/json");
		response.setStatus(200);
		
		JSONObject jsonObjRes = new JSONObject();
		
		// Get the request body, this is json
		StringBuilder bodyIn = new StringBuilder();
		String line = null;
		BufferedReader reader = request.getReader();
		while ((line = reader.readLine()) != null) {
			bodyIn.append(line);
		}
		String requestBody = bodyIn.toString();
		
		logger.info("REQUEST=BODY============================================================");
		logger.info(requestBody);
		logger.info("============================================================");
		
		// Extract the info from the json
		
		JSONObject jsonObj;
		try {
			jsonObj = new JSONObject(requestBody);
			//log.append("Data is valid json");
			
		} catch(Exception e) {
			logger.error("Invalid JSON", e);
			return createErrorResponse(response, 400, false, false, "Error invalid JSON", 101, null);
		}
		
        String userId = jsonObj.optString("uid");
        String accessToken = jsonObj.optString("atk");
        
        System.err.println("In getTracks API - checking userId passed in - " + userId);
        System.err.println("In getTracks API - checking accessToken passed in - " + accessToken);
        
		
		if (Util.isEmpty(userId) || Util.isEmpty(accessToken)) {
			System.err.println("Error in TrackManagerApi - userId or accessToken are empty");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters", 103, null);
		}
		

		Boolean check = FirebaseUtil.isValidToken(userId, accessToken);
        System.out.println("Token Check - " + check);

		if (!check) {
			System.err.println("Error in TrackManagerApi - userId/accessToken not valid");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters - not correct", 103, null);
		}


		
//		UserTracks userTracks = DatastoreManager.getUserTracks(userId);
//		userTracks.getTracks();
		
		List<Tracks> userTracksList = DatastoreManager.getAllTracksForUser(userId);
		System.err.println("In library - usertracks - " + userTracksList.size());
		
		
		if (userTracksList.size() == 0) {
			jsonObjRes.put("success", true);
			jsonObjRes.put("tracks", "");
			return jsonObjRes;
		}
		
///		if (!Util.isEmpty(tracks)) {
///			jsonObjRes.put("success", true);
	//		jsonObjRes.put("trackURL", mubertTrack.getDownload_link());
///			jsonObjRes.put("tracks", userTracks.getTracks());
///			return jsonObjRes;
///		}

		
		JSONArray userTracksJsonArray = new JSONArray();
		
		Collections.sort(userTracksList);
		
		for (Tracks track : userTracksList) {
			JSONObject jsonObjTrack = new JSONObject();
			jsonObjTrack.put("displayName", track.getDisplayName());
			jsonObjTrack.put("fullPath", track.getFullPath());
			jsonObjTrack.put("trackId", track.getId());
			jsonObjTrack.put("category", track.getCategory());
			jsonObjTrack.put("group", track.getGroup());
			jsonObjTrack.put("style", track.getCategory() + ": " + track.getGroup());
			jsonObjTrack.put("duration", track.getDuration());
			jsonObjTrack.put("format",  track.getFormatForDisplay());
	        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy");
	        String formattedDateString = simpleDateFormat.format(track.getDateCreated());

			jsonObjTrack.put("createdDate", formattedDateString);
			userTracksJsonArray.put(jsonObjTrack);
		}
		
		jsonObjRes.put("success", true);
		jsonObjRes.put("tracks", userTracksJsonArray);
		return jsonObjRes;

	}

	public static JSONObject renameTrack(HttpServletRequest request, HttpServletResponse response) throws Exception {
		response.setContentType("application/json");
		response.setStatus(200);
		
		JSONObject jsonObjRes = new JSONObject();
		
		// Get the request body, this is json
		StringBuilder bodyIn = new StringBuilder();
		String line = null;
		BufferedReader reader = request.getReader();
		while ((line = reader.readLine()) != null) {
			bodyIn.append(line);
		}
		String requestBody = bodyIn.toString();
		
		logger.info("REQUEST=BODY============================================================");
		logger.info(requestBody);
		logger.info("============================================================");
		
		// Extract the info from the json
		
		JSONObject jsonObj;
		try {
			jsonObj = new JSONObject(requestBody);
			//log.append("Data is valid json");
			
		} catch(Exception e) {
			logger.error("Invalid JSON", e);
			return createErrorResponse(response, 400, false, false, "Error invalid JSON", 101, null);
		}
		
		// Make sure the user is logged in
		User user = new User(request.getSession());
		if (!user.isLoggedIn()) {
			return createErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, true, false, "Invalid User", 102, null);
		}
		
		// If this user does not have a Plan, send them to Sign Out
		if (!Util.doesUsersHavePlan(user.getFirebase_id())) {
			return createErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, true, false, "Invalid User", 102, null);
		}

		
		String trackName = jsonObj.optString("trackName");
		System.err.println("Parsed out trackName: "+trackName);
		String trackId = jsonObj.optString("trackId");
		System.err.println("Parsed out trackId: "+trackId);

        String userId = jsonObj.optString("uid");
        String accessToken = jsonObj.optString("atk");
        
        System.out.println("In CreateTrackAPI - checking userId passed in - " + userId);
        System.out.println("In CreateTrackAPI - checking accessToken passed in - " + accessToken);
        
		if (Util.isEmpty(trackName) || Util.isEmpty(trackId) || Util.isEmpty(userId) || Util.isEmpty(accessToken)) {
			System.err.println("Error in TrackManagerApi:renameTrack - trackName or trackId or userId or accessToken are empty");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters", -1, null);
		}
		

		Boolean check = FirebaseUtil.isValidToken(userId, accessToken);
        System.out.println("Token Check - " + check);

		if (!check) {
			System.err.println("Error in TrackManagerApi - userId/accessToken not valid");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters - not correct", -1, null);
		}


		Tracks track = DatastoreManager.getTrack(trackId);
		track.setDisplayName(trackName);
		DatastoreManager.putTrack(track);

		System.out.println("In library - renameTrack - " + trackName);
		
		jsonObjRes.put("success", true);
		return jsonObjRes;

	}
	
	
	public static JSONObject renameRevampedTrack(HttpServletRequest request, HttpServletResponse response) throws Exception {
		response.setContentType("application/json");
		response.setStatus(200);
		
		JSONObject jsonObjRes = new JSONObject();
		
		// Get the request body, this is json
		StringBuilder bodyIn = new StringBuilder();
		String line = null;
		BufferedReader reader = request.getReader();
		while ((line = reader.readLine()) != null) {
			bodyIn.append(line);
		}
		String requestBody = bodyIn.toString();
		
		logger.info("REQUEST=BODY============================================================");
		logger.info(requestBody);
		logger.info("============================================================");
		
		// Extract the info from the json
		
		JSONObject jsonObj;
		try {
			jsonObj = new JSONObject(requestBody);
			//log.append("Data is valid json");
			
		} catch(Exception e) {
			logger.error("Invalid JSON", e);
			return createErrorResponse(response, 400, false, false, "Error invalid JSON", 101, null);
		}
		
		String trackName = jsonObj.optString("trackName");
		System.err.println("Parsed out trackName: "+trackName);
		String trackId = jsonObj.optString("trackId");
		System.err.println("Parsed out trackId: "+trackId);

        String userId = jsonObj.optString("uid");
        String accessToken = jsonObj.optString("atk");
        
        System.out.println("In CreateTrackAPI - checking userId passed in - " + userId);
        System.out.println("In CreateTrackAPI - checking accessToken passed in - " + accessToken);
        
		if (Util.isEmpty(trackName) || Util.isEmpty(trackId) || Util.isEmpty(userId) || Util.isEmpty(accessToken)) {
			System.err.println("Error in TrackManagerApi:renameTrack - trackName or trackId or userId or accessToken are empty");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters", 103, null);
		}
		

		Boolean check = FirebaseUtil.isValidToken(userId, accessToken);
        System.out.println("Token Check - " + check);

		if (!check) {
			System.err.println("Error in TrackManagerApi - userId/accessToken not valid");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters - not correct", 103, null);
		}


		TracksRevamp tracksRevamp = DatastoreManager.getTracksRevamp(trackId);
		tracksRevamp.setDisplayName(trackName);
		DatastoreManager.putTracksRevamp(tracksRevamp);

		System.out.println("In library - renameRevampTrack - " + trackName);
		
		jsonObjRes.put("success", true);
		return jsonObjRes;

	}

	public static JSONObject saveTrack(HttpServletRequest request, HttpServletResponse response) throws Exception {
		response.setContentType("application/json");
		response.setStatus(200);
		
		JSONObject jsonObjRes = new JSONObject();
		
		// Get the request body, this is json
		StringBuilder bodyIn = new StringBuilder();
		String line = null;
		BufferedReader reader = request.getReader();
		while ((line = reader.readLine()) != null) {
			bodyIn.append(line);
		}
		String requestBody = bodyIn.toString();
		
		logger.info("REQUEST=BODY============================================================");
		logger.info(requestBody);
		logger.info("============================================================");
		
		// Extract the info from the json
		
		JSONObject jsonObj;
		try {
			jsonObj = new JSONObject(requestBody);
			//log.append("Data is valid json");
			
		} catch(Exception e) {
			logger.error("Invalid JSON", e);
			return createErrorResponse(response, 400, false, false, "Error invalid JSON", 101, null);
		}
		
		// Make sure the user is logged in
		User user = new User(request.getSession());
		if (!user.isLoggedIn()) {
			return createErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, true, false, "Invalid User", 102, null);
		}
		
		// If this user does not have a Plan, send them to Sign Out
		if (!Util.doesUsersHavePlan(user.getFirebase_id())) {
			return createErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, true, false, "Invalid User", 102, null);
		}
		
		
		String trackId = jsonObj.optString("trackId");
		System.err.println("Parsed out trackId: "+trackId);

        String userId = jsonObj.optString("uid");
        String accessToken = jsonObj.optString("atk");
        
        System.out.println("In CreateTrackAPI - checking userId passed in - " + userId);
        System.out.println("In CreateTrackAPI - checking accessToken passed in - " + accessToken);
        
		if (Util.isEmpty(trackId) || Util.isEmpty(userId) || Util.isEmpty(accessToken)) {
			System.err.println("Error in TrackManagerApi:saveTrack - trackId or userId or accessToken are empty");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters", 103, null);
		}
		

		Boolean check = FirebaseUtil.isValidToken(userId, accessToken);
        System.out.println("Token Check - " + check);

		if (!check) {
			System.err.println("Error in TrackManagerApi - userId/accessToken not valid");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters - not correct", 103, null);
		}

//		SqlUtil.getInstance().saveTrack(trackId, userId);
		
		DatastoreManager.saveUserTrack(trackId);
		
		// Update the users count
		// Since SAVE is now the the track default, no need to update counts here
//		Users users = DatastoreManager.getUsers(userId);
//		if (users != null) {
//			if (users.getPlan() != null) {
//				users.setTotalTracksSaved(users.getTotalTracksSaved() + 1);
//				users.setTotalTracksDiscarded(users.getTotalTracksDiscarded() - 1);
//				users.setQuotaTracksSaved(users.getQuotaTracksSaved() + 1);
//				users.setQuotaTracksDiscarded(users.getQuotaTracksDiscarded() - 1);
//				users.save();
//			}
//		}

		jsonObjRes.put("success", true);
		return jsonObjRes;

	}
	
	public static JSONObject discardTrack(HttpServletRequest request, HttpServletResponse response) throws Exception {
		response.setContentType("application/json");
		response.setStatus(200);
		
		JSONObject jsonObjRes = new JSONObject();
		
		// Get the request body, this is json
		StringBuilder bodyIn = new StringBuilder();
		String line = null;
		BufferedReader reader = request.getReader();
		while ((line = reader.readLine()) != null) {
			bodyIn.append(line);
		}
		String requestBody = bodyIn.toString();
		
		logger.info("REQUEST=BODY============================================================");
		logger.info(requestBody);
		logger.info("============================================================");
		
		// Extract the info from the json
		
		JSONObject jsonObj;
		try {
			jsonObj = new JSONObject(requestBody);
			//log.append("Data is valid json");
			
		} catch(Exception e) {
			logger.error("Invalid JSON", e);
			return createErrorResponse(response, 400, false, false, "Error invalid JSON", 101, null);
		}
		
		// Make sure the user is logged in
		User user = new User(request.getSession());
		if (!user.isLoggedIn()) {
			return createErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, true, false, "Invalid User", 102, null);
		}
		
		// If this user does not have a Plan, send them to Sign Out
		if (!Util.doesUsersHavePlan(user.getFirebase_id())) {
			return createErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, true, false, "Invalid User", 102, null);
		}
		
		String trackId = jsonObj.optString("trackId");
		System.err.println("Parsed out trackId: "+trackId);

        String userId = jsonObj.optString("uid");
        String accessToken = jsonObj.optString("atk");
        
        System.out.println("In DiscardTrackAPI - checking userId passed in - " + userId);
        System.out.println("In DiscardTrackAPI - checking accessToken passed in - " + accessToken);
        
		if (Util.isEmpty(trackId) || Util.isEmpty(userId) || Util.isEmpty(accessToken)) {
			logger.error("discardTrack - trackId or userId or accessToken are empty");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters", 103, null);
		}
		

		Boolean check = FirebaseUtil.isValidToken(userId, accessToken);
        System.out.println("Token Check - " + check);

		if (!check) {
			logger.error("discardTrack - userId/accessToken not valid");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters - not correct", 103, null);
		}

//		SqlUtil.getInstance().saveTrack(trackId, userId);
		
		// Make sure the track exists and belongs to this user
        Tracks tracks = DatastoreManager.getTrack(trackId);
        if (tracks == null) {
			logger.error("discardTrack - trackId does not exist - trackId - " + trackId);
			return createErrorResponse(response, 400, false, false, "Error invalid parameters", 103, null);
        } else {
			if (!tracks.getUserId().equalsIgnoreCase(userId)) {
				logger.error("discardTrack - trackId does not belong to user - userId - " + userId + "   trackId - " + trackId);
				return createErrorResponse(response, 400, false, false, "Error invalid parameters", 103, null);
			}
        }

		
		DatastoreManager.discardUserTrack(trackId);
		
		// Update the users count
		// Since SAVE is the track default, we have to update some counts here
		Users users = DatastoreManager.getUsers(userId);
		if (users != null) {
			if (users.getPlan() != null) {
				users.setTotalTracksSaved(users.getTotalTracksSaved() - 1);
				users.setTotalTracksDiscarded(users.getTotalTracksDiscarded() +1);
				users.setQuotaTracksSaved(users.getQuotaTracksSaved() - 1);
				users.setQuotaTracksDiscarded(users.getQuotaTracksDiscarded() + 1);
				users.save();
			}
		}

		jsonObjRes.put("success", true);
		return jsonObjRes;

	}

	public static JSONObject deleteTrack(HttpServletRequest request, HttpServletResponse response) throws Exception {
		response.setContentType("application/json");
		response.setStatus(200);
		
		JSONObject jsonObjRes = new JSONObject();
		
		// Get the request body, this is json
		StringBuilder bodyIn = new StringBuilder();
		String line = null;
		BufferedReader reader = request.getReader();
		while ((line = reader.readLine()) != null) {
			bodyIn.append(line);
		}
		String requestBody = bodyIn.toString();
		
		logger.info("REQUEST=BODY============================================================");
		logger.info(requestBody);
		logger.info("============================================================");
		
		// Extract the info from the json
		
		JSONObject jsonObj;
		try {
			jsonObj = new JSONObject(requestBody);
			//log.append("Data is valid json");
			
		} catch(Exception e) {
			logger.error("Invalid JSON", e);
			return createErrorResponse(response, 400, false, false, "Error invalid JSON", 101, null);
		}
		
		// Make sure the user is logged in
		User user = new User(request.getSession());
		if (!user.isLoggedIn()) {
			return createErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, true, false, "Invalid User", 102, null);
		}
		
		// If this user does not have a Plan, send them to Sign Out
		if (!Util.doesUsersHavePlan(user.getFirebase_id())) {
			return createErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, true, false, "Invalid User", 102, null);
		}

		// Get the users object
		Users users = DatastoreManager.getUsers(user.getFirebase_id());

		String trackId = jsonObj.optString("trackId");
		System.err.println("Parsed out trackId: "+trackId);
		
		String trackOrigin = jsonObj.optString("trackOrigin");

        String userId = jsonObj.optString("uid");
        String accessToken = jsonObj.optString("atk");
        
        System.out.println("In CreateTrackAPI - checking userId passed in - " + userId);
        System.out.println("In CreateTrackAPI - checking accessToken passed in - " + accessToken);
        
        // Make sure we have a trackId and the user and token information
		if (Util.isEmpty(trackId) || Util.isEmpty(trackOrigin) || Util.isEmpty(userId) || Util.isEmpty(accessToken)) {
			logger.error("deleteTrack - trackId or trackOrigin or userId or accessToken are empty");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters", 103, null);
		}
		
		// Make sure trackOrigin is one of the 2 valid values
		if (!(trackOrigin.equalsIgnoreCase(TRACK_ORIGIN_CREATE) || trackOrigin.equalsIgnoreCase(TRACK_ORIGIN_REVAMP))) {
			logger.error("deleteTrack - trackOrigin not valid");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters", 103, null);
		}
		

		Boolean check = FirebaseUtil.isValidToken(userId, accessToken);
        System.out.println("Token Check - " + check);

        // Make sure the userid / accessToken are valid
        if (!check) {
        	logger.error("deleteTrack - userId/accessToken not valid");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters - not correct", 103, null);
		}

        // Make sure this trackId exists
        // Make sure the trackId belongs to this user
        // If so then delete the track
        // We need to know the difference between CREATE and REVAMP
        
        if (trackOrigin.equalsIgnoreCase(TRACK_ORIGIN_CREATE)) {
        	
	        Tracks tracks = DatastoreManager.getTrack(trackId);
	        if (tracks == null) {
	        	logger.error("deleteTrack - trackId does not exist - trackId - " + trackId);
				return createErrorResponse(response, 400, false, false, "Error invalid parameters", 103, null);
	        } else {
				if (!tracks.getUserId().equalsIgnoreCase(userId)) {
					logger.error("deleteTrack - trackId does not belong to user - userId - " + userId + "   trackId - " + trackId);
					return createErrorResponse(response, 400, false, false, "Error invalid parameters", 103, null);
				}
	        }
	        
			DatastoreManager.deleteUserTrack(trackId);
			
        } else {
        	
    		TracksRevamp tracksRevamp = DatastoreManager.getTracksRevamp(trackId);
	        if (tracksRevamp == null) {
	        	logger.error("deleteTrack - trackId does not exist - trackId - " + trackId);
				return createErrorResponse(response, 400, false, false, "Error invalid parameters", 103, null);
	        } else {
	        	
	        	String trackUploadId = tracksRevamp.getTrackUploadId();
	        	TracksUpload tracksUpload = DatastoreManager.getTracksUpload(trackUploadId);
    		
				if (!tracksUpload.getUserId().equalsIgnoreCase(userId)) {
					logger.error("deleteTrack - revamp trackId does not belong to user - userId - " + userId + "   trackId - " + trackId);
					return createErrorResponse(response, 400, false, false, "Error invalid parameters", 103, null);
				}
	        }
	        
	        DatastoreManager.deleteUserTrackRevamp(trackId);
        }
        

		jsonObjRes.put("success", true);
		return jsonObjRes;

	}
	
	public static JSONArray getAllTracksForUser(String userId) throws Exception {
		
		List<Tracks> userTracksList = DatastoreManager.getAllTracksForUser(userId);
		logger.info("In library - usertracks - " + userTracksList.size());
		Collections.sort(userTracksList);
		
		JSONArray userTracksJsonArray = new JSONArray();
		
		for (Tracks track : userTracksList) {
			JSONObject jsonObjTrack = new JSONObject();
			jsonObjTrack.put("displayName", track.getDisplayName());
			jsonObjTrack.put("fullPath", track.getFullPath());
			jsonObjTrack.put("trackId", track.getId());
			jsonObjTrack.put("category", track.getCategory());
			jsonObjTrack.put("group", track.getGroup());
			jsonObjTrack.put("style", track.getCategory() + ": " + track.getGroup());
			jsonObjTrack.put("duration", track.getDuration());
			jsonObjTrack.put("discardFlag", track.getDiscardFlag());
	        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy");
	        String formattedDateString = simpleDateFormat.format(track.getDateCreated());

			jsonObjTrack.put("createdDate", formattedDateString);
			userTracksJsonArray.put(jsonObjTrack);
		}
		
		return userTracksJsonArray;

	}


	public static JSONArray getAllSavedTracksForUser(String userId, Boolean includeFullPath) throws Exception {
		
		List<Tracks> userTracksList = DatastoreManager.getAllSavedTracksForUser(userId);
		System.err.println("In library - usertracks - " + userTracksList.size());
		Collections.sort(userTracksList);
		
		JSONArray userTracksJsonArray = new JSONArray();
		
		for (Tracks track : userTracksList) {
			JSONObject jsonObjTrack = new JSONObject();
			jsonObjTrack.put("displayName", track.getDisplayName());
			if (includeFullPath) {
				jsonObjTrack.put("fullPath", track.getFullPath());
			}
			jsonObjTrack.put("trackId", track.getId());
			jsonObjTrack.put("category", track.getCategory());
			jsonObjTrack.put("group", track.getGroup());
			jsonObjTrack.put("textToMusicPrompt",  track.getTextToMusicPrompt());
			if (track.getCreateType().equalsIgnoreCase(CREATE_TRACK_TYPE_TEXTTOMUSIC)) {
//				jsonObjTrack.put("style", "TextToMusic" + " : " + track.getTextToMusicPrompt());
				jsonObjTrack.put("style", "TextToMusic");
			} else {
//				jsonObjTrack.put("style", track.getCategory() + " : " + track.getGroup());
				jsonObjTrack.put("style", track.getCategory());
			}
			jsonObjTrack.put("duration", track.getDuration());
			jsonObjTrack.put("intensity", track.getIntensity());
			jsonObjTrack.put("format", track.getFormatForDisplay());
			jsonObjTrack.put("discardFlag", track.getDiscardFlag());
			jsonObjTrack.put("createType", track.getCreateType());
//	        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy");
//            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd, yyyy / HH:mm:ss", Locale.ENGLISH);
//	        String formattedDateString = simpleDateFormat.format(track.getDateCreated());
            
//	        SimpleDateFormat isoFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
	        SimpleDateFormat isoFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	        String formattedDateString = isoFormatter.format(track.getDateCreated());

			jsonObjTrack.put("createdDate", formattedDateString);
			jsonObjTrack.put("dateCreated", track.getDateCreated());
			userTracksJsonArray.put(jsonObjTrack);
		}
		
		return userTracksJsonArray;

	}
	

	public static List<Tracks> getAllSavedTracksForUserAsList(String userId, Boolean includeFullPath) throws Exception {
		
		List<Tracks> userTracksList = DatastoreManager.getAllSavedTracksForUser(userId);
		logger.info("In library - usertracks - " + userTracksList.size());
		Collections.sort(userTracksList);
		
		return userTracksList;
		
	}

	public static JSONArray getAllUploadedTracksForUser(String userId) throws Exception {
		
		List<TracksUpload> userTracksList = DatastoreManager.getAllUploadedTracksForUser(userId);
		logger.info("In TrackManagerApi - getAllUploadedTracksForUser - " + userTracksList.size());
		Collections.sort(userTracksList);
		
		JSONArray userTracksJsonArray = new JSONArray();
		
		for (TracksUpload track : userTracksList) {
			JSONObject jsonObjTrack = new JSONObject();
			jsonObjTrack.put("displayName", track.getDisplayName());
			jsonObjTrack.put("fullPath", track.getFullPath());
			jsonObjTrack.put("trackId", track.getId());
			jsonObjTrack.put("genre", track.getGenre());
			jsonObjTrack.put("duration", track.getDuration());
	        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy");
	        String formattedDateString = simpleDateFormat.format(track.getDateCreated());

			jsonObjTrack.put("createdDate", formattedDateString);
			jsonObjTrack.put("dateCreated", track.getDateCreated());
			userTracksJsonArray.put(jsonObjTrack);
		}
		
		return userTracksJsonArray;

	}

	
	public static JSONArray getAllUploadedAndRevampedTracksForUserOrig(String userId) throws Exception {
		
		List<TracksUpload> userTracksUploadList = DatastoreManager.getAllUploadedTracksForUser(userId);
		logger.info("In TrackManagerApi - getAllUploadedAndRevampedTracksForUser - " + userId);
		Collections.sort(userTracksUploadList);
		
		JSONArray userTracksJsonArray = new JSONArray();
		
		for (TracksUpload trackUpload : userTracksUploadList) {
			JSONObject jsonObjTrack = new JSONObject();
			jsonObjTrack.put("type",  "Original");
			jsonObjTrack.put("displayName", trackUpload.getDisplayName());
			jsonObjTrack.put("fullPath", trackUpload.getFullPath());
			jsonObjTrack.put("trackId", trackUpload.getId());
			jsonObjTrack.put("genre", trackUpload.getGenre());
			jsonObjTrack.put("duration", trackUpload.getDuration());
			jsonObjTrack.put("status",  trackUpload.getStatus());
	        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy");
	        String formattedDateString = simpleDateFormat.format(trackUpload.getDateCreated());

			jsonObjTrack.put("dateCreated", formattedDateString);
			
			List<TracksRevamp> userTracksRevampList = DatastoreManager.getAllRevampedTracksForUploadTrack(trackUpload.getId());
			JSONArray userRevampTracksJsonArray = new JSONArray();

			for (TracksRevamp trackRevamp : userTracksRevampList) {
				JSONObject jsonRevampTrack = new JSONObject();
				jsonRevampTrack.put("displayName", trackRevamp.getDisplayName());
				jsonRevampTrack.put("fullPath", trackRevamp.getFullPath());
				jsonRevampTrack.put("id", trackRevamp.getId());
				jsonRevampTrack.put("duration", trackRevamp.getDuration());
		        formattedDateString = simpleDateFormat.format(trackRevamp.getDateCreated());

		        jsonRevampTrack.put("dateCreated", formattedDateString);
			
				userRevampTracksJsonArray.put(jsonRevampTrack);
			}
			jsonObjTrack.put("revampedTracks",  userRevampTracksJsonArray);
			userTracksJsonArray.put(jsonObjTrack);
		}
		
		return userTracksJsonArray;

	}


	public static JSONArray getAllUploadedAndRevampedTracksForUser(String userId) throws Exception {
		
		List<TracksUpload> userTracksUploadList = DatastoreManager.getAllUploadedTracksForUser(userId);
		logger.info("In TrackManagerApi - getAllUploadedAndRevampedTracksForUser - " + userId);
		Collections.sort(userTracksUploadList);
		
		JSONArray userTracksJsonArray = new JSONArray();
		
		for (TracksUpload trackUpload : userTracksUploadList) {
			JSONObject jsonObjTrack = new JSONObject();
			jsonObjTrack.put("type",  "original");
			jsonObjTrack.put("displayName", trackUpload.getDisplayName());
			jsonObjTrack.put("fullPath", trackUpload.getFullPath());
			jsonObjTrack.put("trackId", trackUpload.getId());
			jsonObjTrack.put("genre", trackUpload.getGenre());
			jsonObjTrack.put("duration", trackUpload.getDuration());
			jsonObjTrack.put("status",  trackUpload.getStatus());
	        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy");
	        String formattedDateString = simpleDateFormat.format(trackUpload.getDateCreated());

			jsonObjTrack.put("dateCreated", formattedDateString);
			
			userTracksJsonArray.put(jsonObjTrack);

			List<TracksRevamp> userTracksRevampList = DatastoreManager.getAllRevampedTracksForUploadTrack(trackUpload.getId());
			JSONArray userRevampTracksJsonArray = new JSONArray();

			for (TracksRevamp trackRevamp : userTracksRevampList) {
				JSONObject jsonRevampTrack = new JSONObject();
				jsonRevampTrack.put("type",  "revamped");
				jsonRevampTrack.put("displayName", trackRevamp.getDisplayName());
				jsonRevampTrack.put("fullPath", trackRevamp.getFullPath());
				jsonRevampTrack.put("trackId", trackRevamp.getId());
				jsonRevampTrack.put("genre", trackUpload.getGenre());
				jsonRevampTrack.put("duration", trackRevamp.getDuration());
				jsonRevampTrack.put("status",  "n/a");
		        simpleDateFormat = new SimpleDateFormat("MM-dd-yyyy");
		        formattedDateString = simpleDateFormat.format(trackRevamp.getDateCreated());
		        jsonRevampTrack.put("dateCreated", formattedDateString);

//				jsonRevampTrack.put("displayName", trackRevamp.getDisplayName());
//				jsonRevampTrack.put("fullPath", trackRevamp.getFullPath());
//				jsonRevampTrack.put("id", trackRevamp.getId());
//				jsonRevampTrack.put("duration", trackRevamp.getDuration());
//		        formattedDateString = simpleDateFormat.format(trackRevamp.getDateCreated());
//
//		        jsonRevampTrack.put("dateCreated", formattedDateString);
//			
//				userRevampTracksJsonArray.put(jsonRevampTrack);
				
				userTracksJsonArray.put(jsonRevampTrack);

			}
//			jsonObjTrack.put("revampedTracks",  userRevampTracksJsonArray);
//			userTracksJsonArray.put(jsonObjTrack);
		}
		
		return userTracksJsonArray;

	}


	public static JSONObject uploadTrack(HttpServletRequest request, HttpServletResponse response) throws Exception {
		response.setContentType("application/json");
		response.setStatus(200);
		
		JSONObject jsonObjRes = new JSONObject();
		
		// Get the request body, this is json
		StringBuilder bodyIn = new StringBuilder();
		String line = null;
		BufferedReader reader = request.getReader();
		while ((line = reader.readLine()) != null) {
			bodyIn.append(line);
		}
		String requestBody = bodyIn.toString();
		
		logger.info("REQUEST=BODY============================================================");
		logger.info(requestBody);
		logger.info("============================================================");
		
		// Extract the info from the json
		
		JSONObject jsonObj;
		try {
			jsonObj = new JSONObject(requestBody);
			//log.append("Data is valid json");
			
		} catch(Exception e) {
			logger.error("Invalid JSON", e);
			return createErrorResponse(response, 400, false, false, "Error invalid JSON", 101, null);
		}
		
		String formData = jsonObj.optString("formData");
		
		String trackName = jsonObj.optString("trackName");
		System.err.println("Parsed out trackName: "+trackName);
		String trackId = jsonObj.optString("trackId");
		System.err.println("Parsed out trackId: "+trackId);

        String userId = jsonObj.optString("uid");
        String accessToken = jsonObj.optString("atk");
        
        System.out.println("In uploadTrack - checking userId passed in - " + userId);
        System.out.println("In uploadTrack - checking accessToken passed in - " + accessToken);
        
		if (Util.isEmpty(trackName) || Util.isEmpty(trackId) || Util.isEmpty(userId) || Util.isEmpty(accessToken)) {
			logger.error("uploadTrack - trackName or trackId or userId or accessToken are empty");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters", 103, null);
		}
		

		Boolean check = FirebaseUtil.isValidToken(userId, accessToken);
        System.out.println("Token Check - " + check);

		if (!check) {
			logger.error("uploadTrack - userId/accessToken not valid");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters - not correct", 103, null);
		}


//		Tracks track = DatastoreManager.getTrack(trackId);
//		track.setDisplayName(trackName);
//		DatastoreManager.putTrack(track);

		System.out.println("In library - uploadTrack - " + trackName);
		
		jsonObjRes.put("success", true);
		return jsonObjRes;

	}
	
	public static JSONObject revampTrack(HttpServletRequest request, HttpServletResponse response) throws Exception {
		response.setContentType("application/json");
		response.setStatus(200);
		
		JSONObject jsonObjRes = new JSONObject();
		
		// Get the request body, this is json
		StringBuilder bodyIn = new StringBuilder();
		String line = null;
		BufferedReader reader = request.getReader();
		while ((line = reader.readLine()) != null) {
			bodyIn.append(line);
		}
		String requestBody = bodyIn.toString();
		
		logger.info("REQUEST=BODY============================================================");
		logger.info(requestBody);
		logger.info("============================================================");
		
		// Extract the info from the json
		
		JSONObject jsonObj;
		try {
			jsonObj = new JSONObject(requestBody);
			//log.append("Data is valid json");
			
		} catch(Exception e) {
			logger.error("Invalid JSON", e);
			return createErrorResponse(response, 400, false, false, "Error invalid JSON", 101, null);
		}
		
		String trackUploadId = jsonObj.optString("trackUploadId");
		System.err.println("Parsed out trackUploadId: "+trackUploadId);
		String duration = jsonObj.optString("duration");
		System.err.println("Parsed out duration: "+ duration);
		
		// When we support Tracksy Revamp again we will need to add Intensity
		// For now...
		String intensity = "high";

        String userId = jsonObj.optString("uid");
        String accessToken = jsonObj.optString("atk");
        
        System.out.println("In CreateTrackAPI - checking userId passed in - " + userId);
        System.out.println("In CreateTrackAPI - checking accessToken passed in - " + accessToken);
        
		if (Util.isEmpty(duration) || Util.isEmpty(trackUploadId) || Util.isEmpty(userId) || Util.isEmpty(accessToken)) {
			logger.error("revampTrack - duration or trackId or userId or accessToken are empty");
			return createErrorResponse(response, 400, false, false, "Error invalid parameters", 103, null);
		}
		

		Boolean check = FirebaseUtil.isValidToken(userId, accessToken);
        System.out.println("Token Check - " + check);

		if (!check) {
			logger.error("revampTrack - userId/accessToken not valid");
			return createErrorResponse(response, 400, false, false, "Not authorized", -1, null);
		}
		
		// Check the limit of the RevampUsersBeta
		RevampUsersBeta revampUsersBeta = DatastoreManager.getRevampUsersBeta(userId);
		if (revampUsersBeta == null) {
			logger.error("revampTrack - not a Revamp user:  userId - " + userId);
			return createErrorResponse(response, 400, false, false, "Not authorized", -1, null);
		}
		if (revampUsersBeta.getRevampCount() >= revampUsersBeta.getRevampLimit()) {
			logger.error("revampTrack - no more revamps available:  userId - " + userId + "   uploadCount - " + revampUsersBeta.getUploadCount() + "   uploadLimit - " + revampUsersBeta.getUploadLimit());
			return createErrorResponse(response, 400, false, false, "No more revamps available", -1, null);
		}



//		TracksRevamp tracksRevamp = DatastoreManager.getTracksRevamp(trackId);
//		String trackUploadId = tracksRevamp.getTrackUploadId();
		TracksUpload tracksUpload = DatastoreManager.getTracksUpload(trackUploadId);
		String playlistIndex = tracksUpload.getPlaylistIndex();
		
		
		MubertTrack mubertTrack = MubertApiUtil.createTrack("GenreMood", playlistIndex, "", duration, intensity, "mp3");
		
		// TODO: This is currently throwing an error as the license has expired.  I am hard coding the 1 track below
		// TODO: We need to revert some of this once the license is up and running again
				System.out.println("Created task_id - " + mubertTrack.getTask_id());
				System.out.println("Created task_status_code - " + mubertTrack.getTask_status_code());
				System.out.println("Created task_status_text - " + mubertTrack.getTask_status_text());
				System.out.println("Created track - " + mubertTrack.getDownload_link());
				
				boolean trackDone = false;
//				boolean trackDone = true;
				if (mubertTrack.getTask_status_code() == 2) {
					trackDone = true;
				}
				

		// Keep on checking to make sure track is done every 3 seconds
		// But stop checking after 42 seconds
						
				int trackCheckCount = 0;
				int trackCheckMax = 14;
				boolean trackCheckCountExceeded = false;
				int trackCheckSeconds = 3000;
				while (!trackDone) {
					trackCheckCount++;
					if (trackCheckCount > trackCheckMax) {
						trackCheckCountExceeded = true;
						break;
					}
					boolean checkStatus = MubertApiUtil.isTrackDone(mubertTrack.getTask_id());
					System.out.println("Check Status - " + checkStatus);
					if (checkStatus) {
						trackDone = true;
					}
					Thread.sleep(trackCheckSeconds);
				}

				if (trackCheckCountExceeded) {
					jsonObjRes.put("success", false);
					jsonObjRes.put("error",  "Track Count Check Exceeded");
					jsonObjRes.put("mubertTaskId", mubertTrack.getTask_id());
					return jsonObjRes;
				}

				
				String baseURL = "https://storage.googleapis.com/mmeans_dawbot_tracks/";
				String trackURL = baseURL;
				String trackName = "";
				String displayName = "Revamped Track";
				String category = null;
				String group = null;
				String uuid = UUID.randomUUID().toString();

				try {
					URL url = new URL(mubertTrack.getDownload_link());
			        BufferedInputStream bis = new BufferedInputStream(url.openStream());
			        ByteArrayOutputStream baos = new ByteArrayOutputStream();
			        byte[] buffer = new byte[1024];
			        int count=0;
			        while((count = bis.read(buffer,0,1024)) != -1)
			        {
			            baos.write(buffer, 0, count);
			        }
			        
			        String projectId = "test-project-311421";
			        String bucketName = "mmeans_dawbot_tracks";
			        LocalDateTime instance = LocalDateTime.now();
			        trackName = uuid + ".mp3";
			        trackURL = trackURL + trackName;
			        StorageUtil.uploadDataToBucket(projectId, bucketName, trackName, baos.toByteArray());
			        bis.close();
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
				
				jsonObjRes.put("success", true);
				jsonObjRes.put("trackId", uuid);
				jsonObjRes.put("trackURL", trackURL);
				jsonObjRes.put("trackDisplayName", displayName);
				
				// Update the status of the TracksUpload
				
				tracksUpload.setStatus(TracksUpload.STATUS_COMPLETED);
				DatastoreManager.putTracksUpload(tracksUpload);


				// And update a record in TracksRevamp
				
//				String tracksRevampId = UUID.randomUUID().toString();	
//				TracksRevamp tracksRevampNew = new TracksRevamp(tracksRevampId, trackUploadId, displayName, trackURL, duration);
				TracksRevamp tracksRevampNew = new TracksRevamp(uuid, trackUploadId, displayName, trackURL, duration);
				DatastoreManager.putTracksRevamp(tracksRevampNew);
				
				// And update the RevampUsersBeta
				revampUsersBeta.setRevampCount(revampUsersBeta.getRevampCount() + 1);
				DatastoreManager.putRevampUsersBeta(revampUsersBeta);

				System.out.println("RevampTrack Done - success");
				response.setStatus(200);
				jsonObjRes.put("success", true);
				
				return jsonObjRes;

	}
	
	private static Boolean areDurationIntensityFormatValid(String plan, String duration, String intensity, String format) {
		
		Boolean parametersValid = true;
		
		if (plan.equals(PlanManager.PLAN_FREE)) {
			if (!PlanManager.VALID_FREE_DURATIONS.contains(duration)) {
				return false;
			}
			if (!PlanManager.VALID_FREE_INTENSITIES.contains(intensity)) {
				return false;
			}
			if (!PlanManager.VALID_FREE_FORMATS.contains(format)) {
				return false;
			}
		}
		
		if (plan.equals(PlanManager.PLAN_HOLDMYLICENSE)) {
			if (!PlanManager.VALID_HOLDMYLICENSE_DURATIONS.contains(duration)) {
				return false;
			}
			if (!PlanManager.VALID_HOLDMYLICENSE_INTENSITIES.contains(intensity)) {
				return false;
			}
			if (!PlanManager.VALID_HOLDMYLICENSE_FORMATS.contains(format)) {
				return false;
			}
		}

		if (plan.equals(PlanManager.PLAN_VISIONARY) || plan.equals(PlanManager.PLAN_VISIONARY_ANNUAL)) {
			if (!PlanManager.VALID_VISIONARY_DURATIONS.contains(duration)) {
				return false;
			}
			if (!PlanManager.VALID_VISIONARY_INTENSITIES.contains(intensity)) {
				return false;
			}
			if (!PlanManager.VALID_VISIONARY_FORMATS.contains(format)) {
				return false;
			}
		}

		if (plan.equals(PlanManager.PLAN_CREATOR) || plan.equals(PlanManager.PLAN_CREATOR_ANNUAL)) {
			if (!PlanManager.VALID_CREATOR_DURATIONS.contains(duration)) {
				return false;
			}
			if (!PlanManager.VALID_CREATOR_INTENSITIES.contains(intensity)) {
				return false;
			}
			if (!PlanManager.VALID_CREATOR_FORMATS.contains(format)) {
				return false;
			}
		}
		
		if (plan.equals(PlanManager.PLAN_PROFESSIONAL) || plan.equals(PlanManager.PLAN_PROFESSIONAL_ANNUAL)) {
			if (!PlanManager.VALID_PROFESSIONAL_DURATIONS.contains(duration)) {
				return false;
			}
			if (!PlanManager.VALID_PROFESSIONAL_INTENSITIES.contains(intensity)) {
				return false;
			}
			if (!PlanManager.VALID_PROFESSIONAL_FORMATS.contains(format)) {
				return false;
			}
		}

		return parametersValid;
		
	}

	private static Boolean isTextToMusicPromptValid(String textToMusicPrompt, String textToMusicLanguage) {
		
		Boolean promptValid = true;
		
		if (textToMusicPrompt.length() > 200) {
			return false;
		}
		
        // Define the allowed characters: alphanumeric, spaces, comma, period, single quote, question mark, exclamation point, and hyphen
        String allowedCharsRegex = "^[a-zA-Z0-9 ,.'?!-]*$";
        
        if ("pt".equalsIgnoreCase(textToMusicLanguage)) {
//            allowedCharsRegex = "^[a-zA-Z0-9 ,.'?!-\\u00C0-\\u00FF]*$";
            allowedCharsRegex = "^[\\p{L}\\p{N} ,.'?!-]*$";
        }
//        String allowedCharsRegex = "^[\\p{L}\\p{N} ,.'?!-]*$";
  //      String allowedCharsRegex = "^[a-zA-Z0-9 ,.'?!-\\u00C0-\\u00FF]*$";



        // Check if the input contains any characters not in the approved list
        if (!Pattern.matches(allowedCharsRegex, textToMusicPrompt)) {
        	return false;
        }

		return promptValid;

	}
	
	private static Boolean watermarkTrack(String trackId) {
		
		try {
			URL url = null;
			
			url = new URL("https://us-central1-test-project-311421.cloudfunctions.net/create-watermark-function");
	
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
						
			con.setRequestMethod("POST");
			con.setDoOutput(true);
			con.setRequestProperty("Accept", "application/json");
			con.setRequestProperty("Content-Type", "application/json");
			con.setRequestProperty("User-Agent", "Mozilla/5.0"); // add this line to your code
	
			// Create the JSON message body
			String jsonInputString = "{\"message\": \"" + trackId + "\"}";
	
			// Send the JSON message
			try (OutputStream os = con.getOutputStream()) {
			    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
			    os.write(input, 0, input.length);
			}
	
			// Get the response code
			int responseCode = con.getResponseCode();
			System.out.println("Response Code: " + responseCode);
	
			Boolean success = null;
			// If needed, read the response
			if (responseCode == HttpURLConnection.HTTP_OK) {
			    // Handle success response (e.g., reading the response body)
			    System.out.println("Request was successful.");
			    success = true;
			} else {
			    System.out.println("Request failed.");
			    success = false;
			}
	
			con.disconnect(); // Close the connection when done
			
			return success;
		} catch (Exception e) {
			logger.error("Error calling Watermark File - message - " + e.getMessage());
			return false;
		}
	}
	
	// Helper method to create a standardized error response
	private static JSONObject createErrorResponse(HttpServletResponse response, int statusCode, boolean sessionExpired, boolean success, String errorMessage, int errorCode, Map<String, Object> additionalProperties) {
	    response.setStatus(statusCode);
	    
	    if (sessionExpired) {
	    	response.setHeader("Session-Expired", "true");
	    }
	    
	    JSONObject jsonObjRes = new JSONObject();
	    jsonObjRes.put("success", success);
	    jsonObjRes.put("error", errorMessage);

	    // Only add errorCode if it's not -1
	    if (errorCode != -1) {
	        jsonObjRes.put("errorCode", errorCode);
	    }
	    
	    // Add any additional properties if provided
	    if (additionalProperties != null) {
	        for (Map.Entry<String, Object> entry : additionalProperties.entrySet()) {
	            jsonObjRes.put(entry.getKey(), entry.getValue());
	        }
	    }

	    return jsonObjRes;
	}


}
