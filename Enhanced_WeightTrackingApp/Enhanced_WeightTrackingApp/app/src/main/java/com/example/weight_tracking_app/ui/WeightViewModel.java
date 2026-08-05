package com.example.weight_tracking_app.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.weight_tracking_app.data.WeightRepository;
import com.example.weight_tracking_app.model.User;
import com.example.weight_tracking_app.model.UserProfile;
import com.example.weight_tracking_app.model.WeightEntry;
import com.example.weight_tracking_app.model.WeightSummary;
import com.example.weight_tracking_app.util.SessionManager;

import java.util.List;

public class WeightViewModel extends AndroidViewModel {
    private final WeightRepository repository;
    private final SessionManager session;
    private final MutableLiveData<Integer> currentUserId = new MutableLiveData<>();

    private final LiveData<UserProfile> profile;
    private final LiveData<List<WeightEntry>> entries;
    private final LiveData<User> currentUser;
    private final MutableLiveData<WeightSummary> summary = new MutableLiveData<>();
    private final MutableLiveData<String> events = new MutableLiveData<>();

    public WeightViewModel(@NonNull Application application) {
        super(application);
        repository = new WeightRepository(application);
        session = new SessionManager(application);

        profile = Transformations.switchMap(currentUserId, id ->
                id != null && id > 0 ? repository.observeProfile(id) : new MutableLiveData<UserProfile>());
        entries = Transformations.switchMap(currentUserId, id ->
                id != null && id > 0 ? repository.observeEntries(id) : new MutableLiveData<List<WeightEntry>>());
        currentUser = Transformations.switchMap(currentUserId, id ->
                id != null && id > 0 ? repository.observeUser(id) : new MutableLiveData<User>());

        int existing = session.getUserId();
        if (existing != SessionManager.NO_USER) {
            currentUserId.setValue(existing);
            refreshSummary();
        }
    }

    public LiveData<UserProfile> getProfile() { return profile; }
    public LiveData<List<WeightEntry>> getEntries() { return entries; }
    public LiveData<User> getCurrentUser() { return currentUser; }
    public LiveData<WeightSummary> getSummary() { return summary; }
    public LiveData<String> getEvents() { return events; }
    public void clearEvent() { events.setValue(null); }

    public boolean isLoggedIn() { return session.isLoggedIn(); }
    public int getUserId() { Integer id = currentUserId.getValue(); return id == null ? SessionManager.NO_USER : id; }

    // ---------------- Auth ----------------

    public void login(String username, String password, WeightRepository.Callback<Boolean> callback) {
        repository.login(username, password, userId -> {
            boolean ok = userId != null && userId > 0;
            if (ok) {
                session.setUserId(userId);
                currentUserId.postValue(userId);
                repository.buildSummary(userId, summary::postValue);
            }
            if (callback != null) callback.onComplete(ok);
        });
    }

    public void register(String username, String password, String phone, WeightRepository.Callback<Boolean> callback) {
        repository.register(username, password, phone, userId -> {
            boolean ok = userId != null && userId > 0;
            if (ok) {
                session.setUserId(userId);
                currentUserId.postValue(userId);
                repository.buildSummary(userId, summary::postValue);
            }
            if (callback != null) callback.onComplete(ok);
        });
    }

    public void logout() {
        session.clear();
        currentUserId.setValue(SessionManager.NO_USER);
        summary.setValue(null);
    }

    public void changePassword(String current, String next, WeightRepository.Callback<Boolean> callback) {
        repository.changePassword(getUserId(), current, next, callback);
    }

    public void deleteAccount(Runnable onDone) {
        int id = getUserId();
        repository.deleteAccount(id, () -> {
            session.clear();
            currentUserId.postValue(SessionManager.NO_USER);
            summary.postValue(null);
            if (onDone != null) onDone.run();
        });
    }

    public void sendTestMessage(WeightRepository.Callback<Boolean> callback) {
        repository.sendTestMessage(getUserId(), callback);
    }

    // ---------------- Data ----------------

    public void refreshSummary() {
        int id = getUserId();
        if (id > 0) repository.buildSummary(id, summary::postValue);
    }

    public void saveProfile(UserProfile p, Runnable done) {
        p.id = getUserId();
        repository.saveProfile(p, () -> { refreshSummary(); if (done != null) done.run(); });
    }

    public void addEntry(WeightEntry e, Runnable done) {
        e.userId = getUserId();
        repository.addEntry(e, result -> {
            refreshSummary();
            if (result != null && result.goalJustReached) {
                events.postValue(result.smsSent
                        ? "Goal reached! Congratulations text sent."
                        : "Goal reached! (Add a phone number and grant SMS permission to get a text.)");
            }
            if (done != null) done.run();
        });
    }

    public void updateEntry(WeightEntry e, Runnable done) {
        repository.updateEntry(e, () -> { refreshSummary(); if (done != null) done.run(); });
    }
    public void deleteEntry(int id, Runnable done) {
        repository.deleteEntry(id, () -> { refreshSummary(); if (done != null) done.run(); });
    }
    public void getEntry(int id, WeightRepository.Callback<WeightEntry> callback) { repository.getEntry(id, callback); }
    public void updatePhone(String phone, Runnable done) { repository.updatePhoneNumber(getUserId(), phone, done); }
    public void exportCsv(WeightRepository.Callback<String> callback) { repository.exportEntriesCsv(getUserId(), callback); }
}
