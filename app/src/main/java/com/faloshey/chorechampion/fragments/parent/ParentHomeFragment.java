package com.faloshey.chorechampion.fragments.parent;

import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.faloshey.chorechampion.R;
import com.faloshey.chorechampion.adapters.ChildGridAdapter;
import com.faloshey.chorechampion.models.ChildModel;
import java.util.ArrayList;
import java.util.List;

public class ParentHomeFragment extends Fragment {

    public ParentHomeFragment() { super(R.layout.fragment_parenthome); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.child_account_grid);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(view.getContext(), 2);
        recyclerView.setLayoutManager(gridLayoutManager);

        List<ChildModel> childList = new ArrayList<>();
        // TODO: load from Firestore

        ChildGridAdapter adapter = new ChildGridAdapter(childList);
        recyclerView.setAdapter(adapter);
    }


}
