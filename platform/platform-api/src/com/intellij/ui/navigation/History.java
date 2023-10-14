// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ui.navigation;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.concurrency.annotations.RequiresEdt;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;

public final class History {
  public static final DataKey<History> KEY = DataKey.create("History");

  private final List<Place> myHistory = new ArrayList<>();
  private int myCurrentPos;
  private final Place.Navigator myRoot;

  private boolean myNavigatedNow;
  private final CopyOnWriteArraySet<HistoryListener> myListeners = new CopyOnWriteArraySet<>();

  public History(@NotNull Place.Navigator root) {
    myRoot = root;
  }

  @RequiresEdt
  public void pushQueryPlace() {
    if (isNavigatingNow()) return;

    final Place place = query();
    pushPlace(place);
  }

  private synchronized void pushPlace(@NotNull Place place) {
    while (myCurrentPos >= 0 && myCurrentPos < myHistory.size() - 1) {
      myHistory.remove(myHistory.size() - 1);
    }

    if (!myHistory.isEmpty()) {
      final Place prev = myHistory.get(myHistory.size() - 1);
      if (prev.equals(place)) return;

      if (prev.isMoreGeneralFor(place)) {
        myHistory.remove(prev);
      }
    }

    addPlace(place);
  }

  private synchronized void addPlace(Place place) {
    myHistory.add(place);
    myCurrentPos = myHistory.size() - 1;
  }

  public void pushPlaceForElement(String name, Object value) {
    if (!canNavigateFor(name)) return;

    final Place checkPlace = getCheckPlace(name);
    if (checkPlace == null) return;
    pushPlace(checkPlace.cloneForElement(name, value));
  }

  public void navigateTo(Place place) {
    myRoot.navigateTo(place, false);
  }

  @RequiresEdt
  public void back() {
    int next = findValid(-1);
    assert next != -1;
    goThere(next);
  }

  private int findValid(int increment) {
    List<Place> places = new ArrayList<>();
    int first;
    synchronized (this) {
      first = myCurrentPos + increment;
      for (int idx = first; idx >= 0 && idx < myHistory.size(); idx += increment) {
        places.add(myHistory.get(idx));
      }
    }
    int index = ContainerUtil.indexOf(places, place -> myRoot.isValid(place));
    return index == -1 ? -1 : first + index * increment;
  }

  private void goThere(final int nextPos) {
    myNavigatedNow = true;
    final Place next = myHistory.get(nextPos);
    final Place from = getCurrent();
    fireStarted(from, next);
    try {
      final ActionCallback callback = myRoot.navigateTo(next, false);
      assert callback != null;
      callback.doWhenDone(() -> {
        synchronized (this) {
          myCurrentPos = nextPos;
        }
      }).doWhenProcessed(() -> {
        myNavigatedNow = false;
        fireFinished(from, next);
      });
    }
    catch (Throwable e) {
      myNavigatedNow = false;
      throw new RuntimeException(e);
    }
  }

  public boolean isNavigatingNow() {
    return myNavigatedNow;
  }

  public boolean canGoBack() {
    return findValid(-1) != -1;
  }

  @RequiresEdt
  public void forward() {
    int next = findValid(1);
    assert next != -1;
    goThere(next);
  }

  public boolean canGoForward() {
    return findValid(1) != -1;
  }

  @RequiresEdt
  public synchronized void clear() {
    myHistory.clear();
    myCurrentPos = -1;
  }

  public @NotNull Place query() {
    final Place result = new Place();
    myRoot.queryPlace(result);
    return result;
  }

  private synchronized Place getCurrent() {
    if (myCurrentPos >= 0 && myCurrentPos < myHistory.size()) {
      return myHistory.get(myCurrentPos);
    } else {
      return null;
    }
  }

  private boolean canNavigateFor(String pathElement) {
    if (isNavigatingNow()) return false;

    Place checkPlace = getCheckPlace(pathElement);

    return checkPlace != null && checkPlace.getPath(pathElement) != null;
  }

  private @Nullable Place getCheckPlace(String pathElement) {
    Place checkPlace = getCurrent();
    if (checkPlace == null || checkPlace.getPath(pathElement) == null) {
      checkPlace = query();
    }

    return checkPlace.getPath(pathElement) != null ? checkPlace : null;
  }

  public void addListener(final HistoryListener listener, Disposable parent) {
    myListeners.add(listener);
    Disposer.register(parent, new Disposable() {
      @Override
      public void dispose() {
        myListeners.remove(listener);
      }
    });
  }

  private void fireStarted(Place from, Place to) {
    for (HistoryListener each : myListeners) {
      each.navigationStarted(from, to);
    }
  }

  private void fireFinished(Place from, Place to) {
    for (HistoryListener each : myListeners) {
      each.navigationFinished(from, to);
    }
  }


}
