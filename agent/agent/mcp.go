package agent

import (
	"context"
	"fmt"
	"log"
	"os"

	mcptool "github.com/cloudwego/eino-ext/components/tool/mcp"
	"github.com/cloudwego/eino/components/tool"
	"github.com/mark3labs/mcp-go/client"
	"github.com/mark3labs/mcp-go/mcp"
)

// MCPInstance represents a running IDA MCP process
type MCPInstance struct {
	Client *client.Client
	Tools  []tool.BaseTool
	Status string // "ready", "failed", "stopped"
}

// StartIDAMCP launches idalib-mcp as a subprocess and loads available tools
func StartIDAMCP(ctx context.Context, idaDir string) (*MCPInstance, error) {
	env := append(os.Environ(), "IDADIR="+idaDir)

	log.Printf("[mcp] starting idalib-mcp (IDADIR=%s)", idaDir)
	cli, err := client.NewStdioMCPClient("uv", env, "run", "idalib-mcp", "--stdio")
	if err != nil {
		return nil, fmt.Errorf("start idalib-mcp: %w", err)
	}

	// Initialize MCP protocol
	initReq := mcp.InitializeRequest{}
	initReq.Params.ProtocolVersion = mcp.LATEST_PROTOCOL_VERSION
	initReq.Params.ClientInfo = mcp.Implementation{Name: "bench-agent", Version: "1.0"}
	if _, err := cli.Initialize(ctx, initReq); err != nil {
		log.Printf("[mcp] initialize warning: %v", err)
	}

	// Get tools from MCP server
	rawTools, err := mcptool.GetTools(ctx, &mcptool.Config{Cli: cli})
	if err != nil {
		cli.Close()
		return nil, fmt.Errorf("get MCP tools: %w", err)
	}

	log.Printf("[mcp] loaded %d IDA tools", len(rawTools))
	for _, t := range rawTools {
		info, _ := t.Info(ctx)
		if info != nil {
			log.Printf("[mcp]   - %s", info.Name)
		}
	}

	return &MCPInstance{
		Client: cli,
		Tools:  rawTools,
		Status: "ready",
	}, nil
}

// Stop terminates the MCP subprocess
func (m *MCPInstance) Stop() {
	if m.Client != nil {
		log.Printf("[mcp] stopping idalib-mcp")
		m.Client.Close()
		m.Status = "stopped"
	}
}
