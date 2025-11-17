package com.mmx.tool;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

public class SongFinder {

    private static class Folder {

        String path;
        Vector<Folder> child;
        WeakReference<Folder> parent = null;
        Map<String, String[]> tags;

        String[] songs;

        // child folder
        Folder(Folder parent, String name) {
            assert (parent != null);
            this.parent = new WeakReference<Folder>(parent);
            path = name;
        }

        // root folder, the first level sub folder is static set.
        Folder(String root) {
            path = root;
        }


        String GetFolderPath() {
            if (parent == null) {
                return path;
            }
            Folder upper = this.parent.get();
            assert (upper != null);
            return upper.GetFolderPath() + File.separator + path;
        }

        File OpenFolder() {
            return new File(GetFolderPath());
        }

        void LoadFolder() {
            File dir = OpenFolder();
            child = new Vector<Folder>();
            Vector<String> files = new Vector<String>();
            if (dir.exists()) {
                File[] subs = dir.listFiles();
                if (subs != null) {
                    for (File file : subs) {
                        String name = file.getName();
                        if (!name.isEmpty()) {
                            if (file.isDirectory()) {
                                Folder subFolder = new Folder(this, name);
                                subFolder.LoadFolder();
                                if (subFolder.songs.length > 0 || subFolder.child.size() > 0) {
                                    child.add(subFolder);
                                }
                            } else if (file.isFile() && isFileExtInSupportList(name)) {
                                files.add(name);
                            } else {
                                // do nothing
                            }
                        }
                    }
                }
            }
            songs = files.toArray(new String[0]);
        }

        private boolean isFileExtInSupportList(String name) {
            return true;
        }

        void FilterSong(Filter filter, AcceptAction action) {
            if (filter.Accept(tags)) {
                action.OnAccept(this);
            }

            for (Folder sub : child) {
                sub.FilterSong(filter, action);
            }
        }
    }

    interface Filter {
        boolean Accept(Map<String, String[]> tags);
    }

    interface AcceptAction {
        void OnAccept(Folder folder);
    }

    private static class Root {
        Root(String path) {
            rootPath = path;
        }

        final String rootPath;
        Folder root;
        Folder[] filterResult;

        void Release() {
            root = null;
            filterResult = null;
            System.gc();
        }

        void Load() {
            root = new Folder(rootPath);
            root.LoadFolder();
        }


        void FilterSong(Filter filter) {
            if (root == null) {
                Load();
            }
            final Vector<Folder> result = new Vector<SongFinder.Folder>();
            root.FilterSong(filter, new AcceptAction() {

                public void OnAccept(Folder folder) {
                    if (folder.songs.length > 0) {
                        result.add(folder);
                    }
                }
            });
            filterResult = result.toArray(new Folder[0]);
        }
    }

//    private final String[] rootPath = {
//            "/mnt/1Text4/CAR_BACKUP/music/",
//            "/mnt/1Text4/CAR_BACKUP/03 发烧女声精选200首/"
//    };
    private final String[] rootPath = {
            "/data/local/mmxdisk/JinSun/song",
    };
    private final Vector<Root> roots = new Vector<SongFinder.Root>();

    public void Init() {
        for (String path : rootPath) {
            roots.add(new Root(path));
        }

        Filter allAccept = new Filter() {

            public boolean Accept(Map<String, String[]> tags) {
                return true;
            }
        };
        for (Root root : roots) {
            root.FilterSong(allAccept);
        }
    }

    public void NextSong() {
        String song = null;
        lock.lock();
        song = nextSong;
        nextSong = null;
        lock.unlock();

        if (song == null) {
            int totalNum = 0;
            for (Root root : roots) {
                for (Folder folder : root.filterResult) {
                    totalNum += folder.songs.length;
                }
            }
            if (totalNum > 0) {
                int idx = (int) (Math.random() * totalNum);
                if (idx < 0) {
                    idx = 0;
                }
                if (idx >= totalNum) {
                    idx = totalNum - 1;
                }

                for (Root root : roots) {
                    for (Folder folder : root.filterResult) {
                        if (idx < folder.songs.length) {
                            song = folder.GetFolderPath() + File.separator + folder.songs[idx];
                            break;
                        } else {
                            idx -= folder.songs.length;
                        }
                    }
                    if (song != null) {
                        break;
                    }
                }
            }
        }
        if (song != null) {
            NextSongCallBack cb = null;
            lock.lock();
            if (nextSongCb != null) {
                cb = nextSongCb;
                nextSongCb = null;
            } else {
                nextSong = song;
            }

            lock.unlock();
            if (cb != null) {
                cb.OnNextSong(new File(song));
                NextSong();
            }
        }
    }

    private final ReentrantLock lock = new ReentrantLock();
    private String nextSong;

    public interface NextSongCallBack {
        void OnNextSong(File song);
    }

    private NextSongCallBack nextSongCb;

    public void GetNext(NextSongCallBack cb) {
        File songFile = null;
        lock.lock();
        if (nextSong != null) {
            songFile = new File(nextSong);
            nextSong = null;
            if (!songFile.exists()) {
                nextSongCb = cb;
                songFile = null;
            }
        } else {
            nextSongCb = cb;
        }

        lock.unlock();

        if (songFile != null) {
            cb.OnNextSong(songFile);
        }
    }
}
