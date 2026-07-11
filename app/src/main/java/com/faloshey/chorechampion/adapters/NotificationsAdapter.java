package com.faloshey.chorechampion.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.models.NotificationModel;

import java.util.ArrayList;
import java.util.List;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>{

    private final List<NotificationModel> notificationList;
    private final Context context;

    public NotificationsAdapter(Context context, List<NotificationModel> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        NotificationModel notification = notificationList.get(position);

        holder.txtUser.setText(notification.getUsername());
        holder.txtAction.setText(notification.getActionText());

        GradientDrawable circleBackground = new GradientDrawable();
        circleBackground.setShape(GradientDrawable.OVAL);

        if ("quest_complete".equals(notification.getType())) {

            circleBackground.setColor(Color.parseColor("#E8F5E9"));
            holder.iconContainer.setBackground(circleBackground);

            holder.imgIcon.setImageResource(R.drawable.ic_checkmark);
            holder.imgIcon.setColorFilter(Color.parseColor("#2E7D32"));

        } else if ("purchase_complete".equals(notification.getType())) {

            circleBackground.setColor(Color.parseColor("#FFFDE7"));
            holder.iconContainer.setBackground(circleBackground);

            holder.imgIcon.setImageResource(R.drawable.ic_money_bag);
            holder.imgIcon.setColorFilter(Color.parseColor("#F57F17"));
        } else {

            circleBackground.setColor(Color.parseColor("#F5F5F5"));
            holder.iconContainer.setBackground(circleBackground);
            holder.imgIcon.setImageResource(android.R.drawable.ic_menu_report_image);
            holder.imgIcon.setColorFilter(Color.parseColor("#757575"));
        }
    }

    @Override
    public int getItemCount() {
        Log.d("NotificationDebug", "Adapter size request: " + notificationList.size());
        return notificationList.size();
    }

    public void updateList(List<NotificationModel> newList) {
        this.notificationList.clear();
        this.notificationList.addAll(new ArrayList<>(newList));
        notifyDataSetChanged();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView txtUser, txtAction;
        ImageView imgIcon;
        FrameLayout iconContainer;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUser = itemView.findViewById(R.id.notification_user);
            txtAction = itemView.findViewById(R.id.txt_notification_action);
            imgIcon = itemView.findViewById(R.id.notification_icon);
            iconContainer = itemView.findViewById(R.id.icon_container);
        }
    }
}
