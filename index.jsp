<%@ page import="java.util.*" %>
<%@ page import="com.dawbot.www.*" %>
<%@ page import="org.json.JSONArray" %>


<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%

boolean indexIsAdmin = false;
boolean indexIsRevampUsersBeta = false;
boolean hasUserSeenOptInFlag = false;
boolean showUserOptInOverlay = false;
boolean userIsLoggedIn = false;

%>
<html>
  <head>
  	<%@ include file="/include/common-head.jsp" %>
	<link rel="stylesheet" href="https://cdn.datatables.net/1.13.4/css/jquery.dataTables.min.css" type="text/css" media="all"/>
   	<script src="https://cdn.datatables.net/1.13.4/js/jquery.dataTables.min.js"></script>
  </head>
  
<body>
  
<%@ include file="/include/top-nav.jsp" %>

	<div style="overflow-x: hidden; display: flex; flex-direction: column; justify-content: space-between; min-height: 100vh;">
		<div>
			<div class="homepage_container">
				<div class="homepage_container_2">
					<div class="homepage_container_video">
						<video width="100%" height="100%" loop autoplay muted playsinline src="/images/homepage_customers_53.mp4">
  							<source src="/images/homepage_customers_53.mp4" type="video/mp4">
						</video>
						<div class="absolute inset-0" style="z-index:100; background-color: rgba(0, 0, 0, .49);"></div>
  
					</div>
					<div class="homepage_container_text">
						<div class="homepage_container_text_2">
							<div class="homepage_container_text_3">
								<div class="homepage_container_text_content">
									<h1 style="z-index:200" class="homepage_container_text_content_header">Turn Your Ideas Into Music in Seconds.</h1>
									<a href="#" onclick="goToSamples(event)">Hear Samples</a>&nbsp;&nbsp;|&nbsp;&nbsp;
									<a href="#" onclick="goToTestimonials(event)">Read Testimonials</a>
									
									<h6 style="z-index:200" class="homepage_container_text_content_subheader">Create Professional Sounding Music Free - No Musical Experience Required! Powered by Generative AI.</h6>
									<% if (!userIsLoggedIn) { %>
										<div class="homepageButtons">
											<button class="topnav_button" onclick="location.href='/signup'">Try Free</button>
										</div>
									<% } else { %>
										<div class="homepageButtons">
											<button class="topnav_button" onclick="location.href='/createtrack'">Create Track</button>
										</div>
									<% } %>
									<h6 style="z-index:200" class="homepage_container_text_content_subheader">3 Easy Ways to Create: Just Type Text, Pick a Genre, or Set the Mood!</h6>
									
								</div>
							</div>
						</div>
					</div>


	<section id="tracksy-samples" class="homepage-section">
		<h1 id="sampleheader">Listen To Music Samples Created With Tracksy’s AI!</h1>
		<br>
		  	

		<div id="libraryApp" class="container">
	  		<table v-if="areThereTracks !== null && areThereTracks" id="mytableDT" class="table table-striped table-hover">
	  			<thead>
	  				<tr>
	  					<th scope="col">Style</th>
		  				<th scope="col">Duration</th>
		  				<th scope="col">Intensity</th>
		  				<th scope="col">Format</th>
	  				</tr>
	  			</thead>
	  			<tbody>
	  				<template v-for="(track,index) in tracks">
	  				<tr :id="'trackRow-' + index" @click="changeTrack" :data-tracklocation="track.fullPath" :data-trackname="track.displayName" :data-trackid="track.trackId" :data-trackindex="index" :data-trackcreateddate="track.createdDate">
	  					<td>{{ track.style }}
					        <span v-if="track.createType === 'GenreMood'"> : {{ track.group }}</span>
					        <span v-else data-toggle="tooltip" data-placement="top" :title="track.textToMusicPrompt"> : {{ shortenText(track.textToMusicPrompt) }}</span>
					      </td>
					      <td>{{ track.duration }}</td>
					      <td>{{ track.intensity }}</td>
					      <td>{{ track.format }}</td>
	  				</tr>
	  				</template>
	  			</tbody>
	  		</table>
	  		<div v-if="areThereTracks !== null && !areThereTracks" style="text-align:center">
	  			<h1>You have not yet created any tracks.</h1>
	  			<br/>
				<h2>Please click the button below to create some tracks.</h2>
				<br/>
				<button class="buttoncreate" onclick="location.href='/createtrack'">Create Track</button>
	  		</div>
		</div>
	</section>


<%@ include file="/include/testimonials.jsp" %>


	<% if (!userIsLoggedIn) { %>
		<%@ include file="/include/bottom-tryfreebutton.jsp" %>
	<% } %>

				</div>
			</div>
		</div>
		
<%@ include file="/include/audio-player.jsp" %>
			
<%@ include file="/include/pricing-slidein.jsp" %>
			
<% if (!showUserOptInOverlay) { %>
	<%@ include file="/include/pricing-overlay.jsp" %>
<% } %>

<%@ include file="/include/footer.jsp" %>

	</div>
	
<script>


function changeTrack() {
	
	$('#mytable tr').removeClass('highlight')
	event.currentTarget.className += " highlight";
	
	const trackURL = event.currentTarget.getAttribute("data-tracklocation")
	const trackName = event.currentTarget.getAttribute("data-trackname")
	const trackIndex = event.currentTarget.getAttribute("data-trackindex")
	this.selectedTrackIndex = trackIndex;
	
	changeAudioTrack(trackURL, trackName)

}

function prevNextTrack(direction) {
	if (this.tracks.length < 2) {
		return
	}
	$('#mytable tr').removeClass('highlight')
	var selectedTrackIndex = this.selectedTrackIndex
	if (direction == 'prev') {
		selectedTrackIndex--
	} else {
		selectedTrackIndex++;
	}
	if ((selectedTrackIndex + 1) > this.tracks.length) {
		selectedTrackIndex = 0
	}
	if (selectedTrackIndex < 0) {
		selectedTrackIndex = this.tracks.length - 1
	}
	const track = this.tracks[selectedTrackIndex]
	this.selectedTrackIndex = selectedTrackIndex
	const newRow = $("#mytable").find("[data-trackindex='" + selectedTrackIndex + "']");
	newRow.addClass('highlight')
	changeAudioTrack(track.fullPath, track.displayName)
}


</script>	
	

	<script>

		var libraryApp = new Vue({
    		el: '#libraryApp',
    		data: {
        		isLoggedIn: false,
        		areThereTracks: true,
        		userEmail:'',
        		tracks: [{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/5e47497b-a090-405d-aba6-691de0801438.mp3","duration":"180","intensity":"low","displayName":"New Track","trackId":"5e47497b-a090-405d-aba6-691de0801438","format":"HQ MP3","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"meditation zen "},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/a9297331-13ae-49a8-8b40-1cb4970b86cc.mp3","duration":"45","intensity":"high","displayName":"New Track","trackId":"a9297331-13ae-49a8-8b40-1cb4970b86cc","format":"MP3","style":"Genres","createType":"GenreMood","category":"Genres","group":"Reggaeton","textToMusicPrompt":""},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/97206968-8c55-4320-aafe-f2dc4bbfdb1c.wav","duration":"120","intensity":"medium","displayName":"New Track","trackId":"97206968-8c55-4320-aafe-f2dc4bbfdb1c","format":"HQ WAV","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"tense music for a film scene"},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/e2e3326e-5950-4738-95cc-e69e15d49148.wav","duration":"60","intensity":"low","displayName":"New Track","trackId":"e2e3326e-5950-4738-95cc-e69e15d49148","format":"HQ WAV","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"minor piano "},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/f248ba47-536f-4b05-a10d-8f38d078ac44.wav","duration":"60","intensity":"low","displayName":"New Track","trackId":"f248ba47-536f-4b05-a10d-8f38d078ac44","format":"HQ WAV","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"major guitar "},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/e75c7776-b4e7-45d8-92ee-5a5b9165e549.wav","duration":"180","intensity":"medium","displayName":"New Track","trackId":"e75c7776-b4e7-45d8-92ee-5a5b9165e549","format":"HQ WAV","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"english"},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/a635fd69-62e5-434e-bb53-97d60e2cd9a2.mp3","duration":"180","intensity":"medium","displayName":"New Track","trackId":"a635fd69-62e5-434e-bb53-97d60e2cd9a2","format":"HQ MP3","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"pop, salsa, magic"},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/a0910c1a-31b3-4855-a78f-6e23b4a81f57.mp3","duration":"90","intensity":"high","displayName":"New Track","trackId":"a0910c1a-31b3-4855-a78f-6e23b4a81f57","format":"MP3","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"synthwave, salsa, uptempo"},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/2adc5c62-7c36-4938-8703-3a5b2fdeed63.mp3","duration":"180","intensity":"medium","displayName":"New Track","trackId":"2adc5c62-7c36-4938-8703-3a5b2fdeed63","format":"HQ MP3","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"tech house music deep bass"},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/e6426912-1856-4d19-a214-36b566b51d1f.wav","duration":"180","intensity":"high","displayName":"New Track","trackId":"e6426912-1856-4d19-a214-36b566b51d1f","format":"HQ WAV","style":"Genres","createType":"GenreMood","category":"Genres","group":"Bass"},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/2627a8bf-cadf-465a-888d-3358624d8c86.mp3","duration":"60","intensity":"high","displayName":"New Track","trackId":"2627a8bf-cadf-465a-888d-3358624d8c86","format":"HQ MP3","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"I need music for a dramatic going to battle movie scene "},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/0ab344ca-bf6b-4829-bac0-c09f72b425e2.mp3","duration":"180","intensity":"high","displayName":"New Track","trackId":"0ab344ca-bf6b-4829-bac0-c09f72b425e2","format":"HQ MP3","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"I need a dance cardio vibe for my DJ set at 130 BPM"},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/bd8a843d-b977-4e45-bd43-2b55f87d1cd4.mp3","duration":"105","intensity":"high","displayName":"New Track","trackId":"bd8a843d-b977-4e45-bd43-2b55f87d1cd4","format":"HQ MP3","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"I need music for a movie scene that has an optimistic tone "},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/65c26fbb-cb8c-4f12-b071-3155dc0edd9d.mp3","duration":"120","intensity":"high","displayName":"New Track","trackId":"65c26fbb-cb8c-4f12-b071-3155dc0edd9d","format":"HQ MP3","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"I need some groovy music for a song I am writing "},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/6d736fbb-b277-4d47-ba4b-2fa4ad1fe888.mp3","duration":"150","intensity":"high","displayName":"New Track","trackId":"6d736fbb-b277-4d47-ba4b-2fa4ad1fe888","format":"HQ MP3","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"beautiful tense movie scene"},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/b3ba3840-59bf-4f99-b06f-ca3f8c9d60d3.mp3","duration":"180","intensity":"high","displayName":"New Track","trackId":"b3ba3840-59bf-4f99-b06f-ca3f8c9d60d3","format":"HQ MP3","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"sad summer break up music"},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/a11ce328-43c3-4f53-b43a-e8af90b516e1.mp3","duration":"180","intensity":"high","displayName":"New Track","trackId":"a11ce328-43c3-4f53-b43a-e8af90b516e1","format":"HQ MP3","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"cinematic, acoustic "},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/1d51f6a0-5a28-45ad-961b-58f341cb2630.mp3","duration":"180","intensity":"high","displayName":"New Track","trackId":"1d51f6a0-5a28-45ad-961b-58f341cb2630","format":"HQ MP3","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"cinematic, dramatic flex"},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/f4832f38-7566-4779-b22e-6fbaa51bc1fd.mp3","duration":"165","intensity":"high","displayName":"New Track","trackId":"f4832f38-7566-4779-b22e-6fbaa51bc1fd","format":"HQ MP3","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"horror, hip hop"},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/fb2e66a3-d698-4e05-ada5-13a4363e358a.mp3","duration":"180","intensity":"high","displayName":"New Track","trackId":"fb2e66a3-d698-4e05-ada5-13a4363e358a","format":"HQ MP3","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"house, country, pop, salsa"},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/e407a8a3-83e4-48ad-9d88-1a798f672e56.mp3","duration":"180","intensity":"high","displayName":"New Track","trackId":"e407a8a3-83e4-48ad-9d88-1a798f672e56","format":"MP3","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"jumping to the beat at a festival"},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/fb403c92-5ec4-4224-8aca-ffa8ae0c370b.mp3","duration":"120","intensity":"high","displayName":"New Track","trackId":"fb403c92-5ec4-4224-8aca-ffa8ae0c370b","format":"HQ MP3","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"walking along a trail enjoying the sun in Mexico"},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/3b5ff575-e719-4da3-9844-942f6d7cef03.mp3","duration":"180","intensity":"high","displayName":"New Track","trackId":"3b5ff575-e719-4da3-9844-942f6d7cef03","format":"MP3","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"teen alternative band"},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/806a31e3-3ea2-4ac6-841b-b5c36ce5c29e.mp3","duration":"180","intensity":"high","displayName":"New Track","trackId":"806a31e3-3ea2-4ac6-841b-b5c36ce5c29e","format":"MP3","style":"TextToMusic","createType":"TextToMusic","textToMusicPrompt":"piano chords"},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/597d86ff-fcb2-4df4-824e-299aae1d974a.mp3","duration":"180","intensity":"high","displayName":"New Track","trackId":"597d86ff-fcb2-4df4-824e-299aae1d974a","format":"MP3","style":"Genres","createType":"GenreMood","category":"Genres","group":"Reggaeton"},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/3087a89b-a8bc-4fec-9e03-574fed048fae.mp3","duration":"180","intensity":"high","displayName":"New Track","trackId":"3087a89b-a8bc-4fec-9e03-574fed048fae","format":"HQ MP3","style":"Genres","createType":"GenreMood","category":"Genres","group":"Chill"},{"fullPath":"https://storage.googleapis.com/mmeans_dawbot_tracks/d301a547-b263-47bb-8643-f2601397ff24.wav","duration":"120","intensity":"high","displayName":"New Track","trackId":"d301a547-b263-47bb-8643-f2601397ff24","format":"HQ WAV","style":"Genres","createType":"GenreMood","category":"Genres","group":"Ambient"}],
        		selectedtrack: '',
        		selectedTrackName: '',
        		selectedTrackNewName: '',
        		selectedTrackId: '',
        		selectedTrackIndex: ''
    		},
    		methods: {
    			changeTrack: function() {

					dataTable.$('tr').removeClass('highlight');
					
					event.currentTarget.className += " highlight";
					
    				const trackURL = event.currentTarget.getAttribute("data-tracklocation")
    				var trackName = event.currentTarget.getAttribute("data-trackname")
    				const trackIndex = event.currentTarget.getAttribute("data-trackindex")
    				const trackId = event.currentTarget.getAttribute("data-trackid")
    				this.selectedTrackIndex = trackIndex;
    				
    		        $('#myModal').hide();
					trackName = "Sample Tracks...";

    				changeAudioTrack(trackURL, trackName, true)

    			},
    			prevNextTrack: function(direction) {
    				if (this.tracks.length < 2) {
    					return
    				}

					var visualRows = dataTable.rows({
					    search: 'applied',  // Filter rows based on the current search criteria
					    order: 'applied'    // Sort rows based on the current order criteria
					});
			        // Get an array of row nodes in visual order
			        var visualRowNodes = visualRows.nodes().toArray();
			        var totalVisualRows = visualRowNodes.length

					// If 1 visual row or less, get out of here
			        if (totalVisualRows < 2) {
						return
					}
					
    				var currentRow = dataTable.$('tr.highlight');
    				
    				if (currentRow.length > 0) {
    					
    					var currentIndex = dataTable.row(currentRow).index();
    			        var currentRowNode = currentRow[0]; // Get the DOM element of the currently highlighted row
    			        // Find the index of the currently highlighted row's DOM element in the array of visual row nodes
    			        var currentVisibleIndex = visualRowNodes.indexOf(currentRowNode);
    			        
    			        $(currentRowNode).removeClass('highlight');
    			        
    			        // Calculate the index of the next row
    			        var nextVisibleIndex;
    			        if (direction == 'prev') {
	    			        nextVisibleIndex = currentVisibleIndex - 1
    			        } else {
	    			        nextVisibleIndex = currentVisibleIndex + 1
    			        }
    			        
    			        var currentPage = dataTable.page();
    			        var itemsPerPage = dataTable.page.len();
    			        var startRow = currentPage * itemsPerPage
    			        var endRow = ((currentPage + 1) * itemsPerPage) - 1
    			        if (endRow > totalVisualRows) {
    			        	endRow = totalVisualRows -1 
    			        }
       			        			        
    			        var numPages = dataTable.page.info().pages;
    			        
						var nextRowNode;
	
						// If the next row is still on the page the user is looking at, highlight it
       			        if (nextVisibleIndex >= startRow && nextVisibleIndex <= endRow) {
							nextRowNode = visualRowNodes[nextVisibleIndex];
    			        	$(nextRowNode).addClass('highlight');
    			        	nextRowNode.scrollIntoView({ behavior: 'smooth', block: 'center' });
						// If the next row is past this page, but not past the end, go to the next page
       			        } else if (nextVisibleIndex > endRow && nextVisibleIndex < totalVisualRows) {
    			        	nextRowNode = visualRowNodes[nextVisibleIndex];
    			        	$(nextRowNode).addClass('highlight');
    			        	var nextPage = currentPage + 1;
    			        	dataTable.page(nextPage).draw('page');
    			        	nextRowNode.scrollIntoView({ behavior: 'smooth', block: 'center' });
    			        // If the next row is past the total data, then go back to page 0
       			        } else if (nextVisibleIndex > (totalVisualRows - 1)) {
    			        	nextVisibleIndex = 0;
    			        	nextRowNode = visualRowNodes[nextVisibleIndex];
    			        	$(nextRowNode).addClass('highlight');
    			        	var nextPage = 0;
    			        	dataTable.page(nextPage).draw('page');
    			        	nextRowNode.scrollIntoView({ behavior: 'smooth', block: 'center' });
        			    // If the next row is before the current page, but not before the beginning, then go to previous page
       			        } else if (nextVisibleIndex < startRow && nextVisibleIndex >= 0) {
    			        	nextRowNode = visualRowNodes[nextVisibleIndex];
    			        	$(nextRowNode).addClass('highlight');
    			        	var nextPage = currentPage - 1;
    			        	dataTable.page(nextPage).draw('page');
    			        	nextRowNode.scrollIntoView({ behavior: 'smooth', block: 'center' });
            			// If the next row is before the beginning, then go to the last row of the last page
       			        } else if (nextVisibleIndex < 0) {
    			        	nextVisibleIndex = totalVisualRows - 1;
    			        	nextRowNode = visualRowNodes[nextVisibleIndex];
    			        	$(nextRowNode).addClass('highlight');
    			        	var nextPage = numPages - 1;
    			        	dataTable.page(nextPage).draw('page');
    			        	nextRowNode.scrollIntoView({ behavior: 'smooth', block: 'center' });
       			        }
						
       			     var trackName = $(nextRowNode).data('trackname');
       			     var trackLocation = $(nextRowNode).data('tracklocation');
       			     trackName = "Sample Tracks...";
       			     
       			     changeAudioTrack(trackLocation, trackName, true)
		
		    		}
    			},
        		shortenText: function(fullText) {
        			if (fullText.length > 30) {
        		    	return fullText.slice(0, 30) + '...';
        		    } else {
        		        return fullText;
        		    }
        		}

    		
    		}
		});
		
		
	</script>


<script>

var dataTable;

$(document).ready(function(){
    
	  dataTable = $('#mytableDT').DataTable({
		    columnDefs: [
		    	{ 
		    		targets: 0,
		    		width: '55%'
		    	},
		    	{ 
		    		targets: 1,
		    		width: '15%'
		    	},
		    	{ 
		    		targets: 2,
		    		width: '15%'
		    	},
		    	{ 
		    		targets: 3,
		    		width: '15%'
		    	},
		    ],
		    order: [[0, 'asc']],
		    initComplete: function(settings, json) {
		        // Once the initial sort and other initialization are complete, show the DataTable
		        $('#mytableDT').css('display', 'table');
		        $('#loading-overlay').hide();
//				setTimeout(highlightFirstDataTableRow,500)
		    }

		  });
	  
})

		function highlightFirstDataTableRow() {
			var visualRows = dataTable.rows({
			    search: 'applied',  // Filter rows based on the current search criteria
			    order: 'applied'    // Sort rows based on the current order criteria
			});
			
	        // Get an array of row nodes in visual order
	        var visualRowNodes = visualRows.nodes().toArray();
			nextRowNode = visualRowNodes[0];
        	$(nextRowNode).addClass('highlight');
        	var trackName = $(nextRowNode).data('trackname');
	     	var trackLocation = $(nextRowNode).data('tracklocation');
			trackName = "Sample Tracks...";

   			changeAudioTrack(trackLocation, trackName, false)

		}
		
		function goToSamples(event) {
		    event.preventDefault();
		
		    // Trigger the Mixpanel event
		    mixpanel.track("Hear Samples", {})
		
		    // Scroll to the #customer-testimonials section
		    var samplesSection = document.getElementById('tracksy-samples');
		    if (samplesSection) {
		    	samplesSection.scrollIntoView({ behavior: 'smooth' });
		    }
		
		}

		function goToTestimonials(event) {
		    event.preventDefault();
		
		    // Trigger the Mixpanel event
		    mixpanel.track("Read Testimonials", {})

		    // Scroll to the #customer-testimonials section
		    var testimonialsSection = document.getElementById('customer-testimonials');
		    if (testimonialsSection) {
		        testimonialsSection.scrollIntoView({ behavior: 'smooth' });
		    }
		
		}



mixpanel.track('Home Page', {
  'visited': true
});
</script>


<!-- Opt In Modal -->

<div class="modal" id="optInModal" tabindex="-1" role="dialog" aria-labelledby="optInModalLabel" aria-hidden="true" data-backdrop="static" data-keyboard="false">>
  <div class="modal-dialog" role="document">
    <div class="modal-content">
      <div class="modal-header center">
        <h5 class="modal-title" id="optInModalLabel">Thank you for signing up with Tracksy!</h5>
      </div>
      <div class="modal-body">
        <form>
          <div class="form-group">
            Get in on the secrets! Select the blue button below to become an insider, with first dibs on new products, tips, exclusive deals, and all the latest buzz from Tracksy!
          </div>
        </form>
      </div>
      <div class="modal-footer center">
        <button type="button" class="btn btn-primary" onclick="submitOptIn(true)">Yes, I'm in!</button>
        <a class="greylink" onclick="submitOptIn(false)">No thank you</a>
<%--        <button type="button" class="btn btn-secondary" data-dismiss="modal">No thank you</button> --%>
      </div>
    </div>
  </div>
</div>


	<script>
		<% if (showUserOptInOverlay) { %>
			$('#optInModal').modal('show');
			// If seeing the overlay don't show the other overlay
//			setCookie('pricingOverlaySeen', 'true', 365)
		<% } %>
		
		function submitOptIn(optInValue) {
			$('#optInModal').modal('hide');
		    window.location = "/processOptin?selection=" + optInValue
		}
		
</script>    



 </body>
</html>