package com.qube.piprapay_tool.Forwarding_Class;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;
import com.qube.piprapay_tool.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    private final ArrayList<ForwardingConfig> dataSet;
    private final Context context;
    final Toast[] currentToast = new Toast[1];
    public ListAdapter(ArrayList<ForwardingConfig> data, Context context) {
        this.dataSet = data;
        this.context = context;
    }

    // ✅ NEW METHOD: Add a config and notify adapter
    public void addItem(ForwardingConfig config) {
        dataSet.add(config);
        notifyItemInserted(dataSet.size() - 1);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView sender, url, template, headers, switchSmsLabel;
        AppCompatCheckBox switchSmsOnOff;
        View editButton, deleteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            sender = itemView.findViewById(R.id.text_sender);
            url = itemView.findViewById(R.id.text_url);
            template = itemView.findViewById(R.id.text_template);
            headers = itemView.findViewById(R.id.text_headers);
            switchSmsOnOff = itemView.findViewById(R.id.switch_sms_on_off);
            switchSmsLabel = itemView.findViewById(R.id.text_sms_on_off);
            editButton = itemView.findViewById(R.id.edit_button);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        return new ViewHolder(view);
    }
    private void animateButtonClick(View textView) {
        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(textView, "scaleX", 1f, 0.9f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(textView, "scaleY", 1f, 0.9f, 1f);
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(200); // Duration of the animation
        animatorSet.start();
    }
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        ForwardingConfig config = dataSet.get(position);

        String senderText = config.getSender();
        String asterisk = context.getString(R.string.asterisk);
        String any = context.getString(R.string.any);
        holder.sender.setText(senderText.equals(asterisk) ? any : senderText);

        holder.url.setText(config.getUrl());
        holder.template.setText(config.getTemplate());
        holder.headers.setText(config.getHeaders());

        holder.switchSmsOnOff.setOnCheckedChangeListener(null); // Remove old listener
        holder.switchSmsOnOff.setChecked(config.getIsSmsEnabled());
        holder.switchSmsLabel.setText(config.getIsSmsEnabled() ? R.string.btn_on : R.string.btn_off);

        holder.switchSmsOnOff.setOnCheckedChangeListener((buttonView, isChecked) -> {
            config.setIsSmsEnabled(isChecked);
            holder.switchSmsLabel.setText(isChecked ? R.string.btn_on : R.string.btn_off);
            config.save();
        });

        holder.editButton.setOnClickListener(v -> {
            animateButtonClick( holder.editButton);
            new ForwardingConfigDialog(
                    context,
                    LayoutInflater.from(context),
                    ListAdapter.this
            ).showEdit(config);
        });

        holder.deleteButton.setOnClickListener(v -> {
            animateButtonClick( holder.deleteButton);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.delete_record);
            String message = context.getString(R.string.confirm_delete);
            message = String.format(message, senderText.equals(asterisk) ? any : senderText);
            builder.setMessage(message);

            builder.setPositiveButton(R.string.btn_delete, (dialog, id) -> {
                String hook_url = holder.url.getText().toString().trim();

                StringRequest stringRequest = new StringRequest(com.android.volley.Request.Method.POST, hook_url,
                        response -> {
                            try {
                                JSONObject jsonResponse = new JSONObject(response);
                                String status = jsonResponse.optString("status");
                                if ("true".equalsIgnoreCase(status)) {
                                    String data_message = jsonResponse.getString("message");
                                    show_custom_tost(data_message);

                                    config.remove();
                                    dataSet.remove(position);
                                    notifyItemRemoved(position);
                                    notifyItemRangeChanged(position, dataSet.size());

                                }else {
                                    String data_message = jsonResponse.getString("message");
                                    show_custom_tost(data_message);
                                }
                            }
                            catch (JSONException e) {e.printStackTrace();show_custom_tost("❗Unable to connect");}
                        },
                        error -> { show_custom_tost("❗Unable to connect");}) {
                    @Override
                    protected Map<String, String> getParams() {
                        Map<String, String> params = new HashMap<>();
                        params.put("d_model",  Build.MODEL);    // Device Model
                        params.put("d_brand", Build.BRAND);   // Device Brand
                        params.put("d_version", Build.VERSION.RELEASE);  // Android Version
                        params.put("d_api_level",Build.VERSION.SDK);  // API Level
                        params.put("connection_status","Disconnected");  // is app is connect or inconnect
                        return params;
                    }
                };

                RequestQueue requestQueue = Volley.newRequestQueue(context);
                requestQueue.add(stringRequest);

            });

            builder.setNegativeButton(R.string.btn_cancel, null);
            builder.show();
        });


    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }
    private void show_custom_tost(String message) {
        View rootView = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay() != null ? ((android.app.Activity) context).findViewById(android.R.id.content) : null;

        if (rootView != null) {
            Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT)
                    .setBackgroundTint(context.getResources().getColor(R.color.main_color))
                    .setTextColor(Color.WHITE)
                    .setAction("Hide", undo -> {if (currentToast[0] != null) currentToast[0].cancel();})
                    .setActionTextColor(context.getResources().getColor(R.color.white))
                    .show();
        } else {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show(); // fallback
        }
        MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.tost_sound);
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
    }
    public ArrayList<String> getAllUrls() {
        ArrayList<String> urls = new ArrayList<>();
        for (ForwardingConfig config : dataSet) {
            urls.add(config.getUrl());
        }
        return urls;
    }
}
