import os
from flask import jsonify, request  # Ensure both jsonify and request are imported
from google.cloud import storage
from io import BytesIO
from pydub import AudioSegment

def hello_http(request):
    """HTTP Cloud Function to process audio watermarking.
    Args:
        request (flask.Request): The request object containing JSON payload.
    Returns:
        A JSON response indicating success or error.
    """
    try:
        # Parse the JSON body from the HTTP request
        request_json = request.get_json(silent=True)
        
        if not request_json or 'message' not in request_json:
            return jsonify({'error': 'No message provided'}), 400
        
        # Extract the message from the JSON body
        track_id = request_json['message']
        print(f"Received message track id: {track_id}")

        # Append .mp3 to the filename
        # input_audio_filename="da15f971-9ba2-4185-86bd-dc0691fd857f.mp3"
        input_audio_filename = f"{track_id}-original.mp3"
        output_audio_filename = f"{track_id}.mp3"

        # Define the Cloud Storage bucket and watermark file details
        bucket_name="mmeans_dawbot_tracks"
        watermark_filename="tracksy_watermark_1.mp3"

        # Initialize the Google Cloud Storage client
        storage_client = storage.Client()

        # Get the Cloud Storage bucket and blob (file) and watermark file
        bucket = storage_client.get_bucket(bucket_name)

        # Load the input MP3 file into memory
        input_blob = bucket.blob(input_audio_filename)

        # If the input file does not exist, then stop processing
        if not input_blob.exists():
            return jsonify({"error": f"File {input_audio_filename} does not exist in the bucket."}), 500
   
        input_mp3_data = input_blob.download_as_bytes()
        input_audio_file = BytesIO(input_mp3_data)

        # Load the watermark MP3 file into memory
        watermark_blob = bucket.blob(watermark_filename)
        watermark_mp3_data = watermark_blob.download_as_bytes()
        watermark_audio_file = BytesIO(watermark_mp3_data)

        # Load the MP3 files into AudioSegment objects for processing
        original = AudioSegment.from_mp3(input_audio_file)
        watermark = AudioSegment.from_mp3(watermark_audio_file)

        # Apply gain to the watermark
        watermark = watermark + 10  # Adjust as needed

        # Overlay the watermark at specified intervals (e.g., after 10s, every 20s)
        start_time = 3000  # 3 seconds (in milliseconds)
        interval = 20000    # Repeat every 20 seconds (in milliseconds)

        combined = original
        duration = len(original)
        current_time = start_time
        while current_time < duration:
            combined = combined.overlay(watermark, position=current_time)
            current_time += interval

        # Convert the processed audio back to bytes in memory
        output_audio_file = BytesIO()
        combined.export(output_audio_file, format="mp3")

        # Seek to the beginning of the BytesIO object before uploading
        output_audio_file.seek(0)

        # Upload the processed file back to Cloud Storage
        output_blob = bucket.blob(output_audio_filename)

        # Check if the output file already exists
        if output_blob.exists():
            return jsonify({"error": f"File {output_audio_filename} already exists. Not overwriting."}), 500

        output_blob.upload_from_file(output_audio_file, content_type="audio/mpeg")
        
        # Make the file publicly accessible
        output_blob.acl.all().grant_read()  # Grants public read access
        output_blob.acl.save()  # Save the ACL settings

        print(f"Processed file saved as {output_audio_filename} in bucket {bucket_name}")

        # Return a success message
        return jsonify({'status': 'success', 'input_file': input_audio_filename, 'output_file': output_audio_filename}), 200

    except Exception as e:
        print(f"Error downloading, processing, or uploading the file: {e}")
        return jsonify({'status': 'error', 'message': str(e)}), 500

