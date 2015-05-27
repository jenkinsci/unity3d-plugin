using UnityEngine;
using System.Collections;
using UnityTest;

[IntegrationTest.DynamicTest("IntegrationTotalFailTests")]
[IntegrationTest.SucceedWithAssertions]
public class IntegrationTotalFailTests_Fail : MonoBehaviour
{
  public void Awake()
  {
    IntegrationTest.Fail();
  }
}