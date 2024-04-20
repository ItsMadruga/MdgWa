package its.madruga.wpp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import its.madruga.wpp.BuildConfig;
import its.madruga.wpp.R;
import its.madruga.wpp.models.AppInfoModel;

public class AppListAdapter extends RecyclerView.Adapter<AppListAdapter.AppViewHolder> {

    private final ArrayList<AppInfoModel> dataSet;
    private final Context mContext;

    public AppListAdapter(ArrayList<AppInfoModel> data, Context context) {
        this.dataSet = data;
        this.mContext = context;
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.apps_list_item, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final AppViewHolder holder, int position) {
        final AppInfoModel dataModel = dataSet.get(position);
        holder.txtPackageName.setText(dataModel.getPackageName());

        var required_s = mContext.getString(R.string.required_version_s);
        var installed_s = mContext.getString(R.string.installed_version_s);

        if (!dataModel.getVersion().equals(BuildConfig.VERSION_NAME)) {
            holder.txtRequiredVersion.setVisibility(View.VISIBLE);
            holder.txtRequiredVersion.setText(String.format(required_s, BuildConfig.VERSION_NAME));
            holder.txtInstalledVersion.setTextColor((mContext.getResources().getColor(android.R.color.holo_orange_light, mContext.getTheme())));
        }
        holder.txtInstalledVersion.setText(String.format(installed_s, dataModel.getVersion()));
        holder.imgIcon.setImageDrawable(dataModel.getIcon());
    }

    @Override
    public int getItemCount() {
        return dataSet.size();
    }

    public static class AppViewHolder extends RecyclerView.ViewHolder {
        TextView txtPackageName;
        TextView txtInstalledVersion;
        TextView txtRequiredVersion;
        ImageView imgIcon;

        public AppViewHolder(View view) {
            super(view);
            txtPackageName = view.findViewById(R.id.whatsapp_package_name);
            txtInstalledVersion = view.findViewById(R.id.whatsapp_version);
            txtRequiredVersion = view.findViewById(R.id.whatsapp_required_version);
            imgIcon = view.findViewById(R.id.whatsapp_icon);
        }
    }
}