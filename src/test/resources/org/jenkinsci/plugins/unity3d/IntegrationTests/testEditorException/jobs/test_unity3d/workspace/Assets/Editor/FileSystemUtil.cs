using System;
using System.IO;
using UnityEngine;
using Ionic.Zip;
using System.Collections.Generic;

public class FileSystemUtil
{
	public FileSystemUtil ()
	{
	}
	
	public static IEnumerable<string> GetFiles(string path) {
		Queue<string> queue = new Queue<string>();
		queue.Enqueue(path);
		while (queue.Count > 0) {
			path = queue.Dequeue();
			if (!Directory.Exists(path)) continue;
			foreach (string subDir in Directory.GetDirectories(path)) {
				queue.Enqueue(subDir);
			}
			string[] files = null;
			files = Directory.GetFiles(path);
			if (files != null) {
				for(int i = 0 ; i < files.Length ; i++) {
					yield return files[i];
				}
			}
		}
	}
	
	public static void EnsureParentExists(string target_dir) {
		 DirectoryInfo parent = Directory.GetParent(target_dir);
		 if (!parent.Exists) {
			 Directory.CreateDirectory(parent.FullName);
		 }
	 }	
	
	public static void copyDirectory(string Src,string Dst) {
		copyDirectory(Src, Dst, new string [] {});	
	}

	public static void copyDirectory(string Src,string Dst,string[] IgnorePatterns) {
		String[] Files;

		if(Dst[Dst.Length-1]!=Path.DirectorySeparatorChar) 
			Dst+=Path.DirectorySeparatorChar;
		if(!Directory.Exists(Dst)) Directory.CreateDirectory(Dst);
		Files=Directory.GetFileSystemEntries(Src);
		foreach(string Element in Files) {
			if (ContainsAny(Element, IgnorePatterns)) continue;
			
			if(Directory.Exists(Element)) 
				copyDirectory(Element,Dst+Path.GetFileName(Element), IgnorePatterns);
			else 
				File.Copy(Element,Dst+Path.GetFileName(Element),true);
		}
	}

	public static void SaveTxtInFile(string _Txt,string _FileName) {
		using (StreamWriter sw = new StreamWriter(_FileName))
		{ 
			sw.Write(_Txt);
		}
	}
	
	public static string ReadTxtInFile(string _FileName) {
		using (StreamReader sr = new StreamReader(_FileName))
		{ 
		   return sr.ReadToEnd();
		}
	}
	
	// returns true if the specfied Element contains at least one of the ContainPatterns, false otherwise
	private static bool ContainsAny (string Element, string[] ContainPatterns)
	{
		bool contains = false;
		foreach(string ContainPattern in ContainPatterns) {
			if (Element.Contains(ContainPattern)) {
				contains = true;
				break;
			}
		}
		return contains;
	}

	public static string GetExternDataPath() {
		if(Application.dataPath.Contains(".app")) //On est sur un exe Mac
			return Application.dataPath + "/../../Data";
		return Application.dataPath + "/../Data";
	}
	
	

	

}


