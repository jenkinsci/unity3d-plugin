using UnityEngine;
using System.Collections;
using UnityTest;

[IntegrationTest.DynamicTest("IntegrationPartialFailTests")]
[IntegrationTest.SucceedWithAssertions]
public class IntegrationPartialFailTests_Pass : MonoBehaviour
{
  public void Awake()
  {
    IntegrationTest.Pass();
  }
}

[IntegrationTest.DynamicTest("IntegrationPartialFailTests")]
[IntegrationTest.SucceedWithAssertions]
public class IntegrationPartialFailTests_Fail : MonoBehaviour
{
  public void Awake()
  {
    IntegrationTest.Fail();
  }
}