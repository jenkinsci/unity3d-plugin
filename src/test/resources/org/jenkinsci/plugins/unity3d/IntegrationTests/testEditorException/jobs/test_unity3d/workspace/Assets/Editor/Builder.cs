using UnityEditor;
using UnityEngine;

class Builder
{
	[MenuItem ("Custom/CI/TestEditorException")]
	static void TestEditorException ()
	{
		throw new System.Exception("Simulated Exception");
	}
}
