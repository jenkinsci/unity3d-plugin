using System;
using System.Text;
using System.IO;
using UnityEditor;
using UnityEngine;
using System.Collections;
using System.Collections.Generic;
using Ionic.Zip;

using Antlr.StringTemplate;
using Antlr.StringTemplate.Language;

class Builder
{
	static string[] SCENES = FindEnabledEditorScenes();
	static string[] COPY_DATA_IGNORE_PATTERNS = new string[] {".svn"};
	static string APP_NAME = "Demo";
	static string TARGET_DIR = "target";

	[MenuItem ("Custom/CI/TestEditorException")]
	static void TestEditorException ()
	{
		throw new System.Exception("Simulated Exception");
	}

	[MenuItem ("Custom/CI/Build Active %&B")]
	public static void PerformActiveBuild () {
		switch(EditorUserBuildSettings.activeBuildTarget) {
			case BuildTarget.StandaloneOSXIntel:
			  PerformMacOSXBuild();
			  break;
			case BuildTarget.StandaloneLinux:
			  PerformLinux32Build();
			  break;
			case BuildTarget.FlashPlayer:
			  PerformFlashBuild();
			  break;
			case BuildTarget.WebPlayer:
			  PerformWebPlayerBuild();
			  break;
			case BuildTarget.StandaloneWindows:
			  PerformWin32Build();
			  break;
			case BuildTarget.iPhone:
			  PerformIOSBuild();
			  break;
			case BuildTarget.Android:
			  PerformAndroidBuild();
			  break;
		}
	}

	[MenuItem ("Custom/CI/Build Mac OS X")]
	static void PerformMacOSXBuild ()
	{   
		 Debug.Log("Calling custom build script for MacOS X");
		 string target_dir = APP_NAME + ".app";
		 GenericBuild(SCENES, TARGET_DIR + "/" + target_dir, BuildTarget.StandaloneOSXIntel, BuildOptions.None, true);
		 CopyDataDirUnderTarget();
		 ZipWithData(target_dir, APP_NAME + "_mac");
	}

	[MenuItem ("Custom/CI/Build Linux32")]
	static void PerformLinux32Build ()
	{   
		PerformGenericLinuxBuild("lin32", BuildTarget.StandaloneLinux);
	}

	[MenuItem ("Custom/CI/Build Linux64")]
	static void PerformLinux64Build ()
	{   
		PerformGenericLinuxBuild("lin64", BuildTarget.StandaloneLinux);
	}

	[MenuItem ("Custom/CI/Build FlashPlayer")]
	public static void PerformFlashBuild ()
	{
		Debug.Log("Calling custom build script for Flash");
		string target_dir = APP_NAME;
		string target_swf = TARGET_DIR + "/" + target_dir + "/" + APP_NAME + ".swf";
		GenericBuild(SCENES, target_swf, BuildTarget.FlashPlayer, BuildOptions.None, true);
		//CopyDataDirUnderTarget();
		// let's package the ActionScript code, if any
		FileSystemUtil.copyDirectory("Temp/StagingArea/Data/ConvertedDotNetCode", TARGET_DIR + "/" + target_dir + "/ActionScript", COPY_DATA_IGNORE_PATTERNS);
		ZipWithoutData(target_dir, APP_NAME + "_flash");
	}

	[MenuItem ("Custom/CI/Build WebPlayer")]
	public static void PerformWebPlayerBuild ()
	{
		Debug.Log("Calling custom build script for WebPlayer");
		string target_dir = APP_NAME;
		GenericBuild(SCENES, TARGET_DIR + "/" + target_dir, BuildTarget.WebPlayer, BuildOptions.None, true);
		CopyDataDirUnderTarget();
		ZipWithoutData(target_dir, APP_NAME + "_webp");
	}
		 
	[MenuItem ("Custom/CI/Build Win32")]
	static void PerformWin32Build ()
	{   
		PerformGenericWindowsBuild ("win32", BuildTarget.StandaloneWindows);
	}

	[MenuItem ("Custom/CI/Build Win64")]
	static void PerformWin64Build ()
	{   
		PerformGenericWindowsBuild ("win64", BuildTarget.StandaloneWindows64);
	}

	static void PerformGenericWindowsBuild (string _WinType, BuildTarget _BuildTarget)
	{
		string subDir = APP_NAME + "_" + _WinType;
		Debug.Log("Calling custom build script for " + _WinType);
		string target_exe = subDir + "/" + APP_NAME + ".exe";
		GenericBuild(SCENES, TARGET_DIR + "/" + target_exe, _BuildTarget, BuildOptions.None, true);
		CopyDataDirUnderTarget();
		ZipWithData(subDir, subDir);
	}

	static void PerformGenericLinuxBuild (string _LinType, BuildTarget _BuildTarget)
	{
		string subDir = APP_NAME + "_" + _LinType;
		Debug.Log("Calling custom build script for " + _LinType);
		string target_exe = subDir + "/" + APP_NAME;
		GenericBuild(SCENES, TARGET_DIR + "/" + target_exe, _BuildTarget, BuildOptions.None, true);
		//CopyDataDirUnderTarget();
		ZipWithoutData(subDir, subDir);
	}

	[MenuItem ("Custom/CI/Build iOS")]
	static void PerformIOSBuild ()
	{   
		 Debug.Log("Calling custom build script for iOS");
		 string target_dir = APP_NAME + "_ios";
		 GenericBuild(SCENES, TARGET_DIR + "/" + target_dir, BuildTarget.iPhone, BuildOptions.None, true);
		 ZipWithoutData(target_dir, target_dir);
	}

	[MenuItem ("Custom/CI/Build Android")]
	static void PerformAndroidBuild ()
	{   
		Debug.Log("Calling custom build script for Android");
		string target_apk = APP_NAME + ".apk";
		GenericBuild(SCENES, TARGET_DIR + "/" + target_apk, BuildTarget.Android, BuildOptions.None, false);
		//ZipWithoutData(target_apk, target_apk);
	}
	
	[MenuItem ("Custom/CI/Test Pre Process")]
	static void PerformPreProcess ()
	{   
		 Debug.Log("Test PreProcess");
		 PreProcess("ignored");
	}

	private static string[] FindEnabledEditorScenes() {
		List<string> EditorScenes = new List<string>();
		foreach(EditorBuildSettingsScene scene in EditorBuildSettings.scenes) {
			if (!scene.enabled) continue;
			EditorScenes.Add(scene.path);
		}
		Debug.Log("SCENES: " + ToString(EditorScenes));
		return EditorScenes.ToArray();
	}
	
	internal static string ToString(IList<string> list) {
		StringBuilder sb = new StringBuilder();
		foreach(string path in list) {
			sb.Append(path);
		}
		return sb.ToString();
	}
		

	private static BuildOptions OverrideBuildOptionsWithUserSettings(BuildOptions _DefaultBuildOptions, bool _FirstRun) {
		BuildOptions buildOptions = _DefaultBuildOptions;
		// to preserve manual XCode project changes
		// only create this if the build was run once before
		// http://answers.unity3d.com/questions/236443/buildpipelinebuildplayer-misnames-pbxuser-file.html
		if(!_FirstRun) {
			buildOptions |= BuildOptions.AcceptExternalModificationsToPlayer;
		}
		if (EditorUserBuildSettings.webPlayerStreamed) {
			buildOptions |= BuildOptions.BuildAdditionalStreamedScenes;
		}
		if (EditorUserBuildSettings.webPlayerOfflineDeployment) {
			buildOptions |= BuildOptions.WebPlayerOfflineDeployment;
		}
		if (EditorUserBuildSettings.development)
		{
			buildOptions |= BuildOptions.Development;
		}
		if (EditorUserBuildSettings.connectProfiler)
		{
			buildOptions |= BuildOptions.ConnectWithProfiler;
		}
		if (EditorUserBuildSettings.allowDebugging)
		{
			buildOptions |= BuildOptions.AllowDebugging;
		}
		if (EditorUserBuildSettings.appendProject)
		{
			buildOptions |= BuildOptions.AcceptExternalModificationsToPlayer;
		}
		if (EditorUserBuildSettings.installInBuildFolder) {
			buildOptions |= BuildOptions.InstallInBuildFolder;
		}
#if UNITY_3_5
		buildOptions |= EditorUserBuildSettings.architectureFlags;
#endif
		return buildOptions;
	}

	static void GenericBuild(string[] scenes, string target_dir, BuildTarget build_target, BuildOptions build_options, bool override_options)
	{
		FileSystemUtil.EnsureParentExists(target_dir);
		if (EditorUserBuildSettings.activeBuildTarget != build_target) {
			EditorUserBuildSettings.SwitchActiveBuildTarget(build_target);
		}
		if (override_options) {
			bool FirstRun = !Directory.Exists(target_dir);
			build_options = OverrideBuildOptionsWithUserSettings(build_options, FirstRun);
		}
		Debug.Log("==> BuildOptions: " + build_options);
		string dir = System.IO.Path.GetDirectoryName(target_dir);
		PreProcess(dir);
		string res = BuildPipeline.BuildPlayer(scenes,target_dir,build_target,build_options);
		if (res.Length > 0) {
			throw new Exception("BuildPlayer failure: " + res);
		}
		PostProcess(dir);
	}
	
	static void PreProcess(string target_dir) {
		Debug.Log("==> PreProcess " + target_dir);
		foreach (DictionaryEntry var in Environment.GetEnvironmentVariables())
			Console.WriteLine("{0}={1}", var.Key, var.Value);
		
		Dictionary<string, string> templateInput = new Dictionary<string, string>();
		string buildNumber = Environment.GetEnvironmentVariable("BUILD_NUMBER");
		string buildId = Environment.GetEnvironmentVariable("BUILD_ID");
		string user = Environment.GetEnvironmentVariable("USER");
		if (buildNumber == null) { // when running from editor...
			buildNumber = DateTime.Now.ToString("yyyyMMddHHmmss"); 
		}
		string buildName = buildNumber;
		if (user != null && user != "jenkins") {
			buildName += "_" + user;
		}

		templateInput.Add("BUILD_NUMBER", buildNumber);
		templateInput.Add("BUILD_NAME", buildName);
		templateInput.Add("BUILD_ID", buildId);

		string templateDir = "EditorTemplates";
		foreach(string relativePath in FileSystemUtil.GetFiles(templateDir)) {
			if (relativePath.Contains(".svn")) continue;
			GenerateFileFromTemplate(templateDir, relativePath, templateInput);	
		}
	}
	
	static void GenerateFileFromTemplate(string templateDir, string path, Dictionary<string,string> dictionary) {
		string targetPath = path.Substring(templateDir.Length + 1);
		StringTemplate ST = new StringTemplate(FileSystemUtil.ReadTxtInFile(path));
		foreach(string key in dictionary.Keys) {
			ST.SetAttribute(key, dictionary[key]);
		}
		FileSystemUtil.EnsureParentExists(targetPath);
		Console.WriteLine("Generating file {0} from {1}.", targetPath, templateDir);
		FileSystemUtil.SaveTxtInFile(ST.ToString(), targetPath);
	}

	// a bit redundant with PostProcessingPlayer...
	static void PostProcess(string target_dir) {
		Debug.Log("==> PostProcess " + target_dir);
	}
		   
	static void CopyDataDirUnderTarget()
	{   
		FileSystemUtil.copyDirectory("Data", TARGET_DIR + "/Data", COPY_DATA_IGNORE_PATTERNS);
	}

	static void ZipWithData(string _TargetFileOrDirName, string _BundleName)
	{   
		string TargetFileOrDirPath = TARGET_DIR + "/" + _TargetFileOrDirName;
		using (ZipFile zip = new ZipFile())
		{
			//zip.UseUnicode= true;  // utf-8
			if(Directory.Exists(TargetFileOrDirPath)) {
				zip.AddDirectory(TargetFileOrDirPath, _TargetFileOrDirName);
			} else {
				zip.AddFile(TargetFileOrDirPath, "");				
			}
			zip.AddDirectory(TARGET_DIR + "/Data", "Data");
			zip.Comment = "This zip was created at " + System.DateTime.Now.ToString("G") ; 
			zip.Save(TARGET_DIR + "/" + _BundleName + ".zip");
		}	  	
	}

	static void ZipWithoutData(string _TargetFileOrDirName, string _BundleName)
	{   
		string TargetFileOrDirPath = TARGET_DIR + "/" + _TargetFileOrDirName;
		using (ZipFile zip = new ZipFile())
		{
			//zip.UseUnicode= true;  // utf-8
			if(Directory.Exists(TargetFileOrDirPath)) {
				zip.AddDirectory(TargetFileOrDirPath, _TargetFileOrDirName);
			} else {
				zip.AddFile(TargetFileOrDirPath, "");				
			}
			zip.Comment = "This zip was created at " + System.DateTime.Now.ToString("G") ; 
			zip.Save(TARGET_DIR + "/" + _BundleName + ".zip");
		}	  	
	}
}
