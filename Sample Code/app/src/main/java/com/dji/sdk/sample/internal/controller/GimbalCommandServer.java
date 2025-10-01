package com.dji.sdk.sample.internal.controller;

import android.util.Log;
import fi.iki.elonen.NanoHTTPD;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import java.util.Map;
import java.io.IOException;

public class GimbalCommandServer extends NanoHTTPD {
    private static final String TAG = "GimbalServer";
    private static final int PORT = 8080;
    
    public GimbalCommandServer() {
        super(PORT);
    }
    
    private Gimbal getGimbal() {
        if (DJISDKManager.getInstance() == null) {
            return null;
        }
        
        if (DJISDKManager.getInstance().getProduct() == null) {
            return null;
        }
        
        if (DJISDKManager.getInstance().getProduct() instanceof Aircraft) {
            return ((Aircraft) DJISDKManager.getInstance().getProduct()).getGimbal();
        }
        
        return DJISDKManager.getInstance().getProduct().getGimbal();
    }
    
    @Override
    public Response serve(IHTTPSession session) {
        Log.d(TAG, "Received HTTP request: " + session.getUri());
        
        Gimbal gimbal = getGimbal();
        
        if (gimbal == null) {
            Log.e(TAG, "Gimbal not available");
            return newFixedLengthResponse(Response.Status.SERVICE_UNAVAILABLE, 
                MIME_PLAINTEXT, "ERROR: Gimbal not connected");
        }
        
        Map<String, String> params = session.getParms();
        
        try {
            // Get pitch and yaw from query parameters, default to 0
            float pitch = params.containsKey("pitch") ? 
                Float.parseFloat(params.get("pitch")) : 0f;
            float yaw = params.containsKey("yaw") ? 
                Float.parseFloat(params.get("yaw")) : 0f;
            
            Log.d(TAG, String.format("Executing gimbal command: pitch=%.2f, yaw=%.2f", pitch, yaw));
            
            // Create rotation command
            Rotation rotation = new Rotation.Builder()
                .mode(RotationMode.RELATIVE_ANGLE)
                .pitch(pitch)
                .yaw(yaw)
                .roll(Rotation.NO_ROTATION)
                .time(0.5f)  // Half second movement for responsive tracking
                .build();
            
            // Execute gimbal rotation
            gimbal.rotate(rotation, error -> {
                if (error != null) {
                    Log.e(TAG, "Gimbal rotation error: " + error.getDescription());
                } else {
                    Log.d(TAG, "Gimbal command executed successfully");
                }
            });
            
            String responseMsg = String.format("OK: Gimbal command sent (pitch=%.2f, yaw=%.2f)", pitch, yaw);
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, responseMsg);
            
        } catch (NumberFormatException e) {
            Log.e(TAG, "Invalid parameter format", e);
            return newFixedLengthResponse(Response.Status.BAD_REQUEST, 
                MIME_PLAINTEXT, "ERROR: Invalid number format");
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error", e);
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, 
                MIME_PLAINTEXT, "ERROR: " + e.getMessage());
        }
    }
}
