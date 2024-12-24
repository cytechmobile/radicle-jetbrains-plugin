package network.radicle.jetbrains.radiclejetbrainsplugin.models;

import network.radicle.jetbrains.radiclejetbrainsplugin.services.RadicleCliService;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RadPatchTest {
    @Test
    public void json() throws Exception {
        final var json = getPatchJson();
        var patch = RadicleCliService.MAPPER.readValue(json, RadPatch.class);

        final var json2 = RadicleCliService.MAPPER.writeValueAsString(patch);

        final var patch2 = RadicleCliService.MAPPER.readValue(json2, RadPatch.class);

        assertThat(patch).usingRecursiveComparison().isEqualTo(patch2);
    }

    @Test
    public void location() throws Exception {
        var loc = new RadDiscussion.Location("path", "type", "commit", 1, 5);
        var json = RadicleCliService.MAPPER.writeValueAsString(loc);
        assertThat(json).isNotBlank().doesNotContain("new");

        var correct = RadicleCliService.MAPPER.writeValueAsString(loc.getMapObject());
        assertThat(correct).contains("new");
    }

    @Test
    public void json2() throws Exception {
        var json = """
        {
          "title": "test1",
          "author": {
            "id": "did:key:z6MkjWioeK34B9tWfaMgiR7niJAvuaExarQhUY35CHqFoh3T"
          },
          "state": {
            "status": "open"
          },
          "target": "delegates",
          "labels": [],
          "merges": {},
          "revisions": {
            "7a454414e607e5f237f4c5f7a1ba1cb80c493927": {
              "id": "7a454414e607e5f237f4c5f7a1ba1cb80c493927",
              "author": {
                "id": "did:key:z6MkjWioeK34B9tWfaMgiR7niJAvuaExarQhUY35CHqFoh3T"
              },
              "description": [
                {
                  "author": "z6MkjWioeK34B9tWfaMgiR7niJAvuaExarQhUY35CHqFoh3T",
                  "timestamp": 1735049781000,
                  "body": "test1",
                  "embeds": []
                }
              ],
              "base": "401adcd5d6bb70e044d47f1651461192d6a2154b",
              "oid": "320c5fafb07d7585625adeb6a50771a4e3bc194b",
              "discussion": {
                "comments": {
                  "6c2af28073f4b557cddb69d27095e395e810870c": {
                    "author": "z6MkjWioeK34B9tWfaMgiR7niJAvuaExarQhUY35CHqFoh3T",
                    "reactions": [],
                    "resolved": false,
                    "body": "line comment 1",
                    "edits": [
                      {
                        "author": "z6MkjWioeK34B9tWfaMgiR7niJAvuaExarQhUY35CHqFoh3T",
                        "timestamp": 1736443702000,
                        "body": "line comment 1",
                        "embeds": []
                      }
                    ],
                    "location": {
                      "commit": "401adcd5d6bb70e044d47f1651461192d6a2154b",
                      "path": "test.txt",
                      "old": null,
                      "new": null
                    }
                  },
                  "710880b2026408084ef217d78a5e28f45fe6c196": {
                    "author": "z6MkjWioeK34B9tWfaMgiR7niJAvuaExarQhUY35CHqFoh3T",
                    "reactions": [],
                    "resolved": false,
                    "body": "yo1",
                    "edits": [
                      {
                        "author": "z6MkjWioeK34B9tWfaMgiR7niJAvuaExarQhUY35CHqFoh3T",
                        "timestamp": 1736443592000,
                        "body": "yo1",
                        "embeds": []
                      }
                    ]
                  },
                  "e024a1d61edfb79e391ea675dcdb404d00e24c00": {
                    "author": "z6MkjWioeK34B9tWfaMgiR7niJAvuaExarQhUY35CHqFoh3T",
                    "reactions": [],
                    "resolved": false,
                    "body": "line comment 1",
                    "edits": [
                      {
                        "author": "z6MkjWioeK34B9tWfaMgiR7niJAvuaExarQhUY35CHqFoh3T",
                        "timestamp": 1736443788000,
                        "body": "line comment 1",
                        "embeds": []
                      }
                    ],
                    "location": {
                      "commit": "401adcd5d6bb70e044d47f1651461192d6a2154b",
                      "path": "test.txt",
                      "old": null,
                      "new": null
                    }
                  }
                },
                "timeline": [
                  "710880b2026408084ef217d78a5e28f45fe6c196",
                  "6c2af28073f4b557cddb69d27095e395e810870c",
                  "e024a1d61edfb79e391ea675dcdb404d00e24c00"
                ]
              },
              "reviews": {},
              "timestamp": 1735049781000,
              "resolves": [],
              "reactions": []
            }
          },
          "assignees": [],
          "timeline": [
            "7a454414e607e5f237f4c5f7a1ba1cb80c493927",
            "710880b2026408084ef217d78a5e28f45fe6c196",
            "6c2af28073f4b557cddb69d27095e395e810870c",
            "e024a1d61edfb79e391ea675dcdb404d00e24c00"
          ],
          "reviews": {}
        }
        """;

        var patch = RadicleCliService.MAPPER.readValue(json, RadPatch.class);
        assertThat(patch).isNotNull();
    }

    // CHECKSTYLE:OFF
    public static String getPatchJson() {
        return """
                {
                  "title": "feat: Open file in browser",
                  "author": {
                    "id": "did:key:z6Mkv7BCz61grUDYDmcQhQVmMdcmZmQu3QjMEuQaW7oxQ4H5"
                  },
                  "state": {
                    "status": "merged",
                    "revision": "19e8adb841e4971438cbb5c2c9aa5afc7f4c4086",
                    "commit": "6567cd61700185ff326ba292deacbc160fef2625"
                  },
                  "target": "delegates",
                  "labels": [
                    "RPB:priority:5300"
                  ],
                  "merges": {
                    "z6MkpaATbhkGbSMysNomYTFVvKG5bnNKYZ2cCamfoHzX9SnL": {
                      "revision": "19e8adb841e4971438cbb5c2c9aa5afc7f4c4086",
                      "commit": "6567cd61700185ff326ba292deacbc160fef2625",
                      "timestamp": 1708930611000
                    }
                  },
                  "revisions": {
                    "19e8adb841e4971438cbb5c2c9aa5afc7f4c4086": {
                      "id": "19e8adb841e4971438cbb5c2c9aa5afc7f4c4086",
                      "author": {
                        "id": "did:key:z6Mkv7BCz61grUDYDmcQhQVmMdcmZmQu3QjMEuQaW7oxQ4H5"
                      },
                      "description": [
                        {
                          "author": "z6Mkv7BCz61grUDYDmcQhQVmMdcmZmQu3QjMEuQaW7oxQ4H5",
                          "timestamp": 1708619320000,
                          "body": "",
                          "embeds": []
                        }
                      ],
                      "base": "62a614e126a632c22cb2a0aede8844cc12adde22",
                      "oid": "6567cd61700185ff326ba292deacbc160fef2625",
                      "discussion": {
                        "comments": {
                          "27db6336ce7fbbc5cfdd6e060d592e7989961b73": {
                            "author": "z6Mkv7BCz61grUDYDmcQhQVmMdcmZmQu3QjMEuQaW7oxQ4H5",
                            "reactions": [],
                            "resolved": false,
                            "body": "dadsa",
                            "edits": [
                              {
                                "author": "z6Mkv7BCz61grUDYDmcQhQVmMdcmZmQu3QjMEuQaW7oxQ4H5",
                                "timestamp": 1709647877000,
                                "body": "dadsa",
                                "embeds": []
                              }
                            ]
                          },
                          "27f49b26bd8b9c3af35b53e703a3ee1f1ad9e6b9": {
                            "author": "z6Mkg5ZTteJyyfVxJ3v6vkAAiFcxK7YX1Y7SeoWJK4Q8qCHw",
                            "reactions": [],
                            "resolved": false,
                            "body": "Github Actions Result: success ✅\\n\\nDetails:\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8044876614\\" target=\\"_blank\\" >Draft Release (8044876614)</a>: success 🟢\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8044876613\\" target=\\"_blank\\" >Build (8044876613)</a>: success 🟢\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007460530\\" target=\\"_blank\\" >Code Quality (8007460530)</a>: success 🟢\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007460532\\" target=\\"_blank\\" >Run End-to-End Tests (8007460532)</a>: success 🟢\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007460529\\" target=\\"_blank\\" >Plugin Verifier (8007460529)</a>: success 🟢\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007460528\\" target=\\"_blank\\" >Run UI Tests (8007460528)</a>: success 🟢\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007449405\\" target=\\"_blank\\" >Build (8007449405)</a>: success 🟢",
                            "edits": [
                              {
                                "author": "z6Mkg5ZTteJyyfVxJ3v6vkAAiFcxK7YX1Y7SeoWJK4Q8qCHw",
                                "timestamp": 1710232314000,
                                "body": "Github Actions Result: success ✅\\n\\nDetails:\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8044876614\\" target=\\"_blank\\" >Draft Release (8044876614)</a>: success 🟢\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8044876613\\" target=\\"_blank\\" >Build (8044876613)</a>: success 🟢\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007460530\\" target=\\"_blank\\" >Code Quality (8007460530)</a>: success 🟢\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007460532\\" target=\\"_blank\\" >Run End-to-End Tests (8007460532)</a>: success 🟢\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007460529\\" target=\\"_blank\\" >Plugin Verifier (8007460529)</a>: success 🟢\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007460528\\" target=\\"_blank\\" >Run UI Tests (8007460528)</a>: success 🟢\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007449405\\" target=\\"_blank\\" >Build (8007449405)</a>: success 🟢",
                                "embeds": []
                              }
                            ]
                          },
                          "2c16fbdac93e5d5c13d2290f82e63ad4c373c6b1": null,
                          "4bb4cdf5d9a055642e8014a6c201b4221754db6a": {
                            "author": "z6Mkv7BCz61grUDYDmcQhQVmMdcmZmQu3QjMEuQaW7oxQ4H5",
                            "reactions": [],
                            "resolved": false,
                            "body": "j",
                            "edits": [
                              {
                                "author": "z6Mkv7BCz61grUDYDmcQhQVmMdcmZmQu3QjMEuQaW7oxQ4H5",
                                "timestamp": 1709726890000,
                                "body": "j",
                                "embeds": []
                              }
                            ]
                          },
                          "60e624c103980990bd920a33529794543a8a26ce": {
                            "author": "z6Mkv7BCz61grUDYDmcQhQVmMdcmZmQu3QjMEuQaW7oxQ4H5",
                            "reactions": [],
                            "resolved": false,
                            "body": "tgdfg",
                            "edits": [
                              {
                                "author": "z6Mkv7BCz61grUDYDmcQhQVmMdcmZmQu3QjMEuQaW7oxQ4H5",
                                "timestamp": 1708935419000,
                                "body": "test",
                                "embeds": []
                              },
                              {
                                "author": "z6Mkv7BCz61grUDYDmcQhQVmMdcmZmQu3QjMEuQaW7oxQ4H5",
                                "timestamp": 1709026802000,
                                "body": "test",
                                "embeds": []
                              },
                              {
                                "author": "z6Mkv7BCz61grUDYDmcQhQVmMdcmZmQu3QjMEuQaW7oxQ4H5",
                                "timestamp": 1709647816000,
                                "body": "tgdfg",
                                "embeds": []
                              }
                            ],
                            "location": {
                              "commit": "62a614e126a632c22cb2a0aede8844cc12adde22",
                              "path": "RadAction.java",
                              "old": null,
                              "new": {
                                "type": "lines",
                                "range": {
                                  "start": 13,
                                  "end": 13
                                }
                              }
                            }
                          },
                          "74e15a3cbb9bf7b86678a44a87c3f25663cdef03": {
                            "author": "z6Mkv7BCz61grUDYDmcQhQVmMdcmZmQu3QjMEuQaW7oxQ4H5",
                            "reactions": [],
                            "resolved": false,
                            "body": "test",
                            "edits": [
                              {
                                "author": "z6Mkv7BCz61grUDYDmcQhQVmMdcmZmQu3QjMEuQaW7oxQ4H5",
                                "timestamp": 1711627151000,
                                "body": "test",
                                "embeds": []
                              }
                            ],
                            "location": {
                              "commit": "6567cd61700185ff326ba292deacbc160fef2625",
                              "path": "src/main/java/network/radicle/jetbrains/radiclejetbrainsplugin/actions/rad/RadAction.java",
                              "old": null,
                              "new": {
                                "type": "lines",
                                "range": {
                                  "start": 210,
                                  "end": 210
                                }
                              }
                            }
                          },
                          "8989b57ac485d0865e41486c920ad3ea871f8fc2": {
                            "author": "z6Mkv7BCz61grUDYDmcQhQVmMdcmZmQu3QjMEuQaW7oxQ4H5",
                            "reactions": [],
                            "resolved": false,
                            "body": "gg",
                            "edits": [
                              {
                                "author": "z6Mkv7BCz61grUDYDmcQhQVmMdcmZmQu3QjMEuQaW7oxQ4H5",
                                "timestamp": 1709026778000,
                                "body": "gg",
                                "embeds": []
                              }
                            ]
                          },
                          "8c9e51be094eeb00ad8fe42d8bd92f7ac07e6667": {
                            "author": "z6Mkg5ZTteJyyfVxJ3v6vkAAiFcxK7YX1Y7SeoWJK4Q8qCHw",
                            "reactions": [],
                            "resolved": false,
                            "body": "Github Actions Workflows 🟧\\n\\nWorkflows:\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8044876614\\" target=\\"_blank\\" >Draft Release (8044876614)</a> 🟠\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8044876613\\" target=\\"_blank\\" >Build (8044876613)</a> 🟠\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007460530\\" target=\\"_blank\\" >Code Quality (8007460530)</a> 🟠\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007460532\\" target=\\"_blank\\" >Run End-to-End Tests (8007460532)</a> 🟠\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007460529\\" target=\\"_blank\\" >Plugin Verifier (8007460529)</a> 🟠\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007460528\\" target=\\"_blank\\" >Run UI Tests (8007460528)</a> 🟠\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007449405\\" target=\\"_blank\\" >Build (8007449405)</a> 🟠",
                            "edits": [
                              {
                                "author": "z6Mkg5ZTteJyyfVxJ3v6vkAAiFcxK7YX1Y7SeoWJK4Q8qCHw",
                                "timestamp": 1710232313000,
                                "body": "Github Actions Workflows 🟧\\n\\nWorkflows:\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8044876614\\" target=\\"_blank\\" >Draft Release (8044876614)</a> 🟠\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8044876613\\" target=\\"_blank\\" >Build (8044876613)</a> 🟠\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007460530\\" target=\\"_blank\\" >Code Quality (8007460530)</a> 🟠\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007460532\\" target=\\"_blank\\" >Run End-to-End Tests (8007460532)</a> 🟠\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007460529\\" target=\\"_blank\\" >Plugin Verifier (8007460529)</a> 🟠\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007460528\\" target=\\"_blank\\" >Run UI Tests (8007460528)</a> 🟠\\n\\n - <a href=\\"https://github.com/cytechmobile/radicle-jetbrains-plugin/actions/runs/8007449405\\" target=\\"_blank\\" >Build (8007449405)</a> 🟠",
                                "embeds": []
                              }
                            ]
                          },
                          "ec0e66a9b5fb2b908419a6e866fb01689f425979": {
                            "author": "z6Mkg5ZTteJyyfVxJ3v6vkAAiFcxK7YX1Y7SeoWJK4Q8qCHw",
                            "reactions": [],
                            "resolved": false,
                            "body": "Checking for Github Actions Workflows.",
                            "edits": [
                              {
                                "author": "z6Mkg5ZTteJyyfVxJ3v6vkAAiFcxK7YX1Y7SeoWJK4Q8qCHw",
                                "timestamp": 1710232252000,
                                "body": "Checking for Github Actions Workflows.",
                                "embeds": []
                              }
                            ]
                          }
                        },
                        "timeline": [
                          "60e624c103980990bd920a33529794543a8a26ce",
                          "8989b57ac485d0865e41486c920ad3ea871f8fc2",
                          "9210c414bcb534494531f774e52713e8057c211e",
                          "c3342d474da122296289c6f9d76c909096a2a9f0",
                          "27db6336ce7fbbc5cfdd6e060d592e7989961b73",
                          "2c16fbdac93e5d5c13d2290f82e63ad4c373c6b1",
                          "5493a2dfc2e8c09f2e424134bae2ec0f4c3409d3",
                          "4bb4cdf5d9a055642e8014a6c201b4221754db6a",
                          "ec0e66a9b5fb2b908419a6e866fb01689f425979",
                          "8c9e51be094eeb00ad8fe42d8bd92f7ac07e6667",
                          "27f49b26bd8b9c3af35b53e703a3ee1f1ad9e6b9",
                          "74e15a3cbb9bf7b86678a44a87c3f25663cdef03"
                        ]
                      },
                      "reviews": {},
                      "timestamp": 1708619320000,
                      "resolves": [],
                      "reactions": []
                    }
                  },
                  "assignees": [],
                  "timeline": [
                    "19e8adb841e4971438cbb5c2c9aa5afc7f4c4086",
                    "a753a102a7581ea3ab1b0e5255e19588501c858c",
                    "60e624c103980990bd920a33529794543a8a26ce",
                    "8989b57ac485d0865e41486c920ad3ea871f8fc2",
                    "9210c414bcb534494531f774e52713e8057c211e",
                    "c3342d474da122296289c6f9d76c909096a2a9f0",
                    "27db6336ce7fbbc5cfdd6e060d592e7989961b73",
                    "2c16fbdac93e5d5c13d2290f82e63ad4c373c6b1",
                    "5493a2dfc2e8c09f2e424134bae2ec0f4c3409d3",
                    "4bb4cdf5d9a055642e8014a6c201b4221754db6a",
                    "ec0e66a9b5fb2b908419a6e866fb01689f425979",
                    "8c9e51be094eeb00ad8fe42d8bd92f7ac07e6667",
                    "27f49b26bd8b9c3af35b53e703a3ee1f1ad9e6b9",
                    "74e15a3cbb9bf7b86678a44a87c3f25663cdef03",
                    "4e5d28f7f3ec5257f699d61642e7f40311e7e2f1"
                  ],
                  "reviews": {}
                }
                """;
    }
}
