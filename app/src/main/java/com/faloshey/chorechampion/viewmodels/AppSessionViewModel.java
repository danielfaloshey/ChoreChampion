package com.faloshey.chorechampion.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.faloshey.chorechampion.models.ChildModel;

public class AppSessionViewModel extends ViewModel {

    private final MutableLiveData<ChildModel> activeChild = new MutableLiveData<>();

    // Sets active child profile when switching to child mode
    public void setActiveChild(ChildModel child) {
        activeChild.setValue(child);
    }

    // Called when switching back to parent mode
    public void clearActiveChild() {
        activeChild.setValue(null);
    }

    public LiveData<ChildModel> getActiveChild() {
        return activeChild;
    }
}
