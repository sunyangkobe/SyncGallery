package com.utoronto.syncgallery.utils;


/**
 * FileManagement is the class to handle files.
 * 
 * @author Kobe Sun, Daniel
 * 
 */

public class ImageManager {

//	private List<Map<String, Object>> list;
//	private Map<String, Object> map;


//	private static List<Map<String, Object>> getFile(String file_path){
//		list = new ArrayList<Map<String, Object>>();
//		File file = new File(file_path);
//		for (File files : file.listFiles()) {
//			map = new HashMap<String, Object>();
//			String file_abs_path = files.getAbsolutePath();
//			map.put("file_abs_path", file_abs_path);
//			if (files.isDirectory()) {
//				map.put("item_name", files.getName());
//				list.add(map);
//			} else {
//				map.put("item_name", files.getName());
//				list.add(map);
//			}
//		}
//		return list;
//	}
	
//	private static ListAdapter getListAdapter(GalleryActivity gallery, String file_path) {
//		return new SimpleAdapter(gallery, getFile(file_path), R.layout.list_items,
//				new String[]{"file_abs_path","item_name"},
//				new int[]{R.id.file_abs_path,R.id.item_name});
//	}
//	
//	public static void show_filelist(GalleryActivity gallery, String title,
//			String msg) {
//		final AlertDialog.Builder builder = new AlertDialog.Builder(gallery);
//		final OnClickListener listener1 = new OnClickListener() {
//			public void onClick(DialogInterface dialog, int which) {
//			}
//		};
//
//		//builder.setAdapter(getListAdapter(gallery, SyncGalleryConstants.Gallery_ENTRY_DIR), new DialogInterface.OnClickListener());
//		builder.setTitle(title);
//		builder.setMessage(msg);
//		builder.setPositiveButton("Ok", listener1);
//		builder.setNegativeButton("Cancel", null);
//		builder.show();
//	}
//		
	
}
