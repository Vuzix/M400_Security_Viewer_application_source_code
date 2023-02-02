/*
Copyright (c) 2019, Vuzix Corporation
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

*  Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

*  Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

*  Neither the name of Vuzix Corporation nor the names of
   its contributors may be used to endorse or promote products derived
   from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.vuzix.securityviewer.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.vuzix.securityviewer.R;
import com.vuzix.securityviewer.model.Camera;

import java.util.List;

public class CameraCardAdapter extends RecyclerView.Adapter<CameraCardAdapter.CameraCard> {

    private List<Camera> cameras;

    /**
     * Constructor for CameraCardAdaptor
     * @param cameras List of camera needed to construct the view holders
     */
    public CameraCardAdapter(List<Camera> cameras){
        this.cameras = cameras;
    }

    /**
     * Method called on view holder creation, used to initialized card view
     * @param parent Used for inflating the view holder
     * @param viewType Used for inflating the view holder
     * @return CameraCard that was created
     */
    @NonNull
    @Override
    public CameraCard onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_slider_card, parent, false);
        CameraCard cameraCard = new CameraCard(view);
        return cameraCard;
    }

    /**
     * Method called when holder is bound
     * @param holder CameraCard holder that has been bound
     * @param position Position of the CameraCard
     */
    @Override
    public void onBindViewHolder(@NonNull CameraCard holder, int position) {
        holder.setCameraName(cameras.get(position).getPresentableName());
        holder.setSelected(cameras.get(position).isSelected());
        //TODO If we want to add a preview of the camera (an image) we need to do it here.
    }

    /**
     * Method for getting the count of cameras
     * @return Size of cameras list
     */
    @Override
    public int getItemCount() {
        return cameras.size();
    }

    class CameraCard extends RecyclerView.ViewHolder{

        TextView cameraName;
        FrameLayout selectedFrame;


        /**
         * Constructor for CameraCard, initializes name and the selected frame
         * @param itemView View for the CameraCard
         */
        CameraCard(@NonNull View itemView) {
            super(itemView);
            cameraName = itemView.findViewById(R.id.card_camera_title);
            selectedFrame = itemView.findViewById(R.id.card_selected_frame);
        }

        /**
         * Setter method for changing the name of the camera on the card
         * @param cameraName Name of the camera to display
         */
        void setCameraName(String cameraName) {
            this.cameraName.setText(cameraName);
        }

        /**
         * Set the selected frame to visible if this camera is marked as selected
         * @param selected Show selected frame
         */
        public void setSelected(boolean selected){
            selectedFrame.setVisibility((selected) ? View.VISIBLE : View.INVISIBLE);
        }

    }
}