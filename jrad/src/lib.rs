use anyhow::anyhow;
use base64::Engine;
use radicle::cob::thread::CommentId;
use radicle::cob::{CodeLocation, DataUri, Embed, Reaction, Uri};
use radicle::git::raw::IntoCString;
use radicle::git::Oid;
use radicle::node::AliasStore;
use radicle::patch::RevisionId;
use radicle::prelude::RepoId;
use radicle::storage::git::Repository;
use radicle::storage::ReadStorage;
use radicle::Profile;
use serde::{Deserialize, Deserializer, Serialize};
use std::collections::HashMap;
use std::ffi::c_char;
use std::str::FromStr;

#[no_mangle]
pub extern "system" fn radHome(_inp: *const c_char) -> *const c_char {
    // let input_res = read_input(inp);
    let p = Profile::load().unwrap();
    let result = p.home.path().to_str().unwrap();
    construct_result(String::from(result))
}

#[no_mangle]
pub extern "system" fn changeIssueTitleDescription(inp: *const c_char) -> *const c_char {
    let result = handle_change_issue_title_description(inp);
    match result {
        Ok(st) => construct_result(st),
        Err(e) => construct_error_result(e),
    }
}

#[no_mangle]
pub extern "system" fn editIssueComment(inp: *const c_char) -> *const c_char {
    let result = handle_edit_issue_comment(inp);
    match result {
        Ok(st) => construct_result(st),
        Err(e) => construct_error_result(e),
    }
}

#[no_mangle]
pub extern "system" fn getEmbeds(inp: *const c_char) -> *const c_char {
    let result = handle_get_embeds(inp);
    match result {
        Ok(st) => construct_result(st),
        Err(e) => construct_error_result(e),
    }
}

#[no_mangle]
pub extern "system" fn getAlias(inp: *const c_char) -> *const c_char {
    let result = handle_get_alias(inp);
    match result {
        Ok(st) => construct_result(st),
        Err(e) => construct_error_result(e),
    }
}

#[no_mangle]
pub extern "system" fn createPatchComment(inp: *const c_char) -> *const c_char {
    let result = handle_create_patch_comment(inp);
    match result {
        Ok(st) => construct_result(st),
        Err(e) => construct_error_result(e),
    }
}

#[no_mangle]
pub extern "system" fn editPatchComment(inp: *const c_char) -> *const c_char {
    let result = handle_edit_patch_comment(inp);
    match result {
        Ok(st) => construct_result(st),
        Err(e) => construct_error_result(e),
    }
}

#[no_mangle]
pub extern "system" fn deletePatchComment(inp: *const c_char) -> *const c_char {
    let result = handle_delete_patch_comment(inp);
    match result {
        Ok(st) => construct_result(st),
        Err(e) => construct_error_result(e),
    }
}

#[no_mangle]
pub extern "system" fn patchCommentReact(inp: *const c_char) -> *const c_char {
    let result = handle_patch_comment_react(inp);
    match result {
        Ok(st) => construct_result(st),
        Err(e) => construct_error_result(e),
    }
}

#[no_mangle]
pub extern "system" fn issueCommentReact(inp: *const c_char) -> *const c_char {
    let result = handle_issue_comment_react(inp);
    match result {
        Ok(st) => construct_result(st),
        Err(e) => construct_error_result(e),
    }
}

#[derive(Serialize, Deserialize, Clone, Debug)]
struct ChangeIssueTitleDesc {
    repo_id: RepoId,
    issue_id: Oid,
    title: String,
    description: String,
    embeds: Vec<ContentEmbed>,
}
#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct ContentEmbed {
    pub oid: Oid,
    pub name: String,
    pub content: String,
}
pub fn handle_change_issue_title_description(inp: *const c_char) -> Result<String, anyhow::Error> {
    let input = read_input(inp)?;
    let req: ChangeIssueTitleDesc = serde_json::from_str(&input)?;
    let p = Profile::load()?;
    let repo = p.storage.repository(req.repo_id)?;
    let signer = p.signer()?;

    let mut issues = p.issues_mut(&repo)?;
    let mut issue = issues.get_mut(&req.issue_id.into())?;

    let _er = issue.edit(req.title, &signer)?;
    if !req.description.is_empty() {
        let embeds = resolve_embeds(&repo, req.embeds);
        let _er2 = issue.edit_description(req.description, embeds, &signer)?;
    }

    Ok(String::from("{\"ok\": true}"))
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct EmbedReq {
    pub repo_id: RepoId,
    pub oids: Vec<Oid>,
}
pub fn handle_get_embeds(inp: *const c_char) -> Result<String, anyhow::Error> {
    let input = read_input(inp)?;
    let req: EmbedReq = serde_json::from_str(input.as_str())?;
    let p = Profile::load()?;
    let repo = p.storage.repository(req.repo_id)?;
    let mut res_map = HashMap::<String, String>::new();
    for oid in req.oids {
        let blob = repo.backend.find_blob(oid.into());
        if blob.is_err() {
            continue;
        }
        let blob = blob?;
        let bytes = blob.content();
        let enc = base64::engine::general_purpose::STANDARD.encode(bytes);
        res_map.insert(oid.to_string(), enc);
    }
    let res_map_json = serde_json::to_string(&res_map)?;
    let result = format!("{{\"ok\": true, \"result\":{res_map_json}}}");
    Ok(result)
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct AliasReq {
    pub ids: Vec<String>,
}
pub fn handle_get_alias(inp: *const c_char) -> Result<String, anyhow::Error> {
    let input = read_input(inp)?;
    let req: AliasReq = serde_json::from_str(input.as_str())?;
    let p = Profile::load()?;

    let mut map = HashMap::<String, String>::new();
    for mut id in req.ids {
        if id.starts_with("did:key:") {
            id = id.replace("did:key:", "");
        }
        let nid = id.clone().as_str().parse();
        if nid.is_err() {
            continue;
        }
        let alias = p.aliases().alias(&nid?);
        if alias.is_some() {
            let alias = alias.unwrap();
            map.insert(id, alias.to_string());
        }
    }
    let res_map_json = serde_json::to_string(&map)?;
    let result = format!("{{\"ok\": true, \"result\":{res_map_json}}}");
    Ok(result)
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct PatchCommentReq {
    pub repo_id: RepoId,
    pub patch_id: Oid,
    pub revision_id: RevisionId,
    #[serde(deserialize_with = "ok_or_none")]
    #[serde(default)]
    pub comment_id: Option<CommentId>, // required when editing
    pub comment: String,
    #[serde(deserialize_with = "ok_or_none")]
    #[serde(default)]
    pub reply_to: Option<CommentId>,
    #[serde(deserialize_with = "ok_or_none")]
    #[serde(default)]
    pub location: Option<CodeLocation>,
    pub embeds: Vec<ContentEmbed>,
}
pub fn handle_create_patch_comment(inp: *const c_char) -> Result<String, anyhow::Error> {
    let input = read_input(inp)?;
    let req: PatchCommentReq = serde_json::from_str(input.as_str())?;
    let p = Profile::load()?;
    let repo = p.storage.repository(req.repo_id)?;
    let mut patches = p.patches_mut(&repo)?;
    let mut patch = patches.get_mut(&req.patch_id.into())?;
    let embeds = resolve_embeds(&repo, req.embeds);
    let cid = patch.comment(
        req.revision_id,
        req.comment,
        req.reply_to,
        req.location,
        embeds,
        &p.signer()?,
    )?;
    let result = format!("{{\"ok\": true, \"result\":\"{cid}\"}}");
    Ok(result)
}
pub fn handle_edit_patch_comment(inp: *const c_char) -> Result<String, anyhow::Error> {
    let input = read_input(inp)?;
    let req: PatchCommentReq = serde_json::from_str(input.as_str())?;
    let p = Profile::load()?;
    let repo = p.storage.repository(req.repo_id)?;
    let mut patches = p.patches_mut(&repo)?;
    let mut patch = patches.get_mut(&req.patch_id.into())?;
    let embeds = resolve_embeds(&repo, req.embeds);
    let comment_id = req
        .comment_id
        .ok_or_else(|| anyhow!("missing comment id"))?;
    let cid = patch.comment_edit(
        req.revision_id,
        comment_id,
        req.comment,
        embeds,
        &p.signer()?,
    )?;
    let result = format!("{{\"ok\": true, \"result\":\"{cid}\"}}");
    Ok(result)
}
pub fn handle_delete_patch_comment(inp: *const c_char) -> Result<String, anyhow::Error> {
    let input = read_input(inp)?;
    let req: PatchCommentReq = serde_json::from_str(input.as_str())?;
    let p = Profile::load()?;
    let repo = p.storage.repository(req.repo_id)?;
    let mut patches = p.patches_mut(&repo)?;
    let mut patch = patches.get_mut(&req.patch_id.into())?;
    let comment_id = req
        .comment_id
        .ok_or_else(|| anyhow!("missing comment id"))?;
    let cid = patch.comment_redact(req.revision_id, comment_id, &p.signer()?)?;
    let result = format!("{{\"ok\": true, \"result\":\"{cid}\"}}");
    Ok(result)
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct IssueCommentReq {
    pub repo_id: RepoId,
    pub issue_id: Oid,
    #[serde(deserialize_with = "ok_or_none")]
    #[serde(default)]
    pub comment_id: Option<CommentId>,
    #[serde(deserialize_with = "ok_or_none")]
    #[serde(default)]
    pub reply_to: Option<CommentId>,
    pub comment: String,
    pub embeds: Vec<ContentEmbed>,
}
pub fn handle_edit_issue_comment(inp: *const c_char) -> Result<String, anyhow::Error> {
    let input = read_input(inp)?;
    let req: IssueCommentReq = serde_json::from_str(input.as_str())?;
    let p = Profile::load()?;
    let repo = p.storage.repository(req.repo_id)?;
    let mut issues = p.issues_mut(&repo)?;
    let mut issue = issues.get_mut(&req.issue_id.into())?;
    let comment_id = req
        .comment_id
        .ok_or_else(|| anyhow!("missing comment id"))?;
    let embeds = resolve_embeds(&repo, req.embeds);
    let id = issue.edit_comment(comment_id, req.comment, embeds, &p.signer()?)?;
    let result = format!("{{\"ok\": true, \"result\":\"{id}\"}}");
    Ok(result)
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct PatchCommentReactReq {
    pub repo_id: RepoId,
    pub patch_id: Oid,
    pub revision_id: RevisionId,
    pub comment_id: CommentId,
    pub reaction: Reaction,
    pub active: bool,
}
pub fn handle_patch_comment_react(inp: *const c_char) -> Result<String, anyhow::Error> {
    let input = read_input(inp)?;
    let req: PatchCommentReactReq = serde_json::from_str(input.as_str())?;
    let p = Profile::load()?;
    let repo = p.storage.repository(req.repo_id)?;
    let mut patches = p.patches_mut(&repo)?;
    let mut patch = patches.get_mut(&req.patch_id.into())?;
    let eid = patch.comment_react(
        req.revision_id,
        req.comment_id,
        req.reaction,
        req.active,
        &p.signer()?,
    )?;
    let result = format!("{{\"ok\": true, \"result\":\"{eid}\"}}");
    Ok(result)
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct IssueCommentReactReq {
    pub repo_id: RepoId,
    pub issue_id: Oid,
    pub comment_id: CommentId,
    pub reaction: Reaction,
    pub active: bool,
}
pub fn handle_issue_comment_react(inp: *const c_char) -> Result<String, anyhow::Error> {
    let input = read_input(inp)?;
    let req: IssueCommentReactReq = serde_json::from_str(input.as_str())?;
    let p = Profile::load()?;
    let repo = p.storage.repository(req.repo_id)?;
    let mut issues = p.issues_mut(&repo)?;
    let mut issue = issues.get_mut(&req.issue_id.into())?;
    let id = issue.react(req.comment_id, req.reaction, req.active, &p.signer()?)?;
    let result = format!("{{\"ok\": true, \"result\":\"{id}\"}}");
    Ok(result)
}

pub fn resolve_embeds(repo: &Repository, embeds: Vec<ContentEmbed>) -> Vec<Embed<Uri>> {
    embeds
        .into_iter()
        .filter_map(|embed| resolve_embed(repo, embed).ok())
        .collect()
}

pub fn resolve_embed(repo: &Repository, embed: ContentEmbed) -> Result<Embed<Uri>, anyhow::Error> {
    let res = repo.backend.find_blob(embed.oid.into());
    if res.is_err() {
        let content_uri = Uri::from_str(embed.content.as_str()).map_err(|e| anyhow!("{}", e))?;
        let content = DataUri::try_from(&content_uri).map_err(|e| anyhow!("{}", e))?;
        let content_vec = Vec::from(content);
        let embedded = Embed::<Oid>::store(&embed.name, content_vec.as_slice(), &repo.backend)?;
        let res: Embed<Uri> = Embed::<Uri> {
            name: embed.name,
            content: embedded.oid().into(),
        };
        Ok(res)
    } else {
        let res: Embed<Uri> = Embed::<Uri> {
            name: embed.name,
            content: embed.oid.into(),
        };
        Ok(res)
    }
}

fn read_input(input: *const c_char) -> Result<String, anyhow::Error> {
    let input_str = unsafe { std::ffi::CStr::from_ptr(input) };
    let input_string = input_str.to_str()?;
    Ok(String::from(input_string))
}

fn construct_result(input: String) -> *const c_char {
    input.into_c_string().unwrap().into_raw()
}

fn construct_error_result(e: anyhow::Error) -> *const c_char {
    construct_result(format!("{{\"ok\": false, \"msg\": \"{e}\"}}"))
}

fn ok_or_none<'de, D, T>(deserializer: D) -> Result<Option<T>, D::Error>
where
    D: Deserializer<'de>,
    T: Deserialize<'de>,
{
    let v = serde_json::Value::deserialize(deserializer)?;
    Ok(T::deserialize(v).ok())
}

#[cfg(test)]
pub mod tests {
    use radicle::cob::migrate;
    use radicle::crypto::ssh::Keystore;
    use radicle::crypto::test::signer::MockSigner;
    use radicle::crypto::{KeyPair, Seed};
    use radicle::node::config::Network;
    use radicle::node::Alias;
    use radicle::profile::{Config, Home};
    use radicle::test::setup::{Node, NodeRepo};
    use radicle::Profile;
    use std::ffi::{CStr, CString};
    use std::path::PathBuf;
    use tempfile::tempdir;

    #[test]
    fn change_issue_with_embeds() {
        let t = generate_test_data();

        let mut issues = t.profile.issues_mut(&t.repo.repo).unwrap();
        let issue = issues
            .create(
                "test_issue_1",
                "test_description_1",
                &[],
                &[],
                [],
                &t.node.signer,
            )
            .unwrap();
        let iid = issue.id().to_string();
        let rid = t.repo.id;

        // prepare input
        let json_input = format!("{{\"repo_id\": \"{rid}\", \"issue_id\":\"{iid}\",\"title\":\"new title 2\",\"description\":\"new description 2\",\"embeds\":[]}}");
        let input_cstr = CString::new(json_input.as_bytes()).unwrap();
        let input_ptr = input_cstr.as_ptr();

        let result_ptr = super::changeIssueTitleDescription(input_ptr);
        let result = unsafe { CStr::from_ptr(result_ptr) }.to_str().unwrap();
        assert_eq!(result, "{\"ok\": true}");
    }

    pub fn generate_test_data() -> TestData {
        let alias = "tester";
        let node = Node::new(tempdir().unwrap(), MockSigner::from_seed([!0; 32]), alias);
        let repo = node.project();
        let home_path = node.root.join("home");
        let home = Home::new(home_path.clone()).unwrap();
        let keystore = Keystore::new(&home.keys());
        let keypair = KeyPair::from_seed(Seed::from([!0; 32]));
        keystore.store(keypair.clone(), alias, None).unwrap();

        // create config as well
        let mut cfg = Config::new(Alias::new(alias));
        cfg.node.network = Network::Test;
        cfg.write(&home.config()).unwrap();

        // set correct home
        std::env::set_var("RAD_HOME", home_path.to_str().unwrap());

        // load profile and migrate db
        let p = Profile::load().unwrap();
        p.cobs_db_mut().unwrap().migrate(migrate::ignore).unwrap();

        TestData {
            node,
            repo,
            home_path,
            home,
            profile: p,
        }
    }

    pub struct TestData {
        pub node: Node,
        pub repo: NodeRepo,
        pub home_path: PathBuf,
        pub home: Home,
        pub profile: Profile,
    }
}
